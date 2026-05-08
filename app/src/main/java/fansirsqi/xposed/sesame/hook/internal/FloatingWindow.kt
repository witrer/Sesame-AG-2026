package fansirsqi.xposed.sesame.hook.internal

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import fansirsqi.xposed.sesame.hook.ApplicationHook
import fansirsqi.xposed.sesame.util.Files
import fansirsqi.xposed.sesame.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 悬浮窗调试助手
 * 功能：抓页面信息、录制RPC、手动执行任务
 */
object FloatingWindow {

    private const val TAG = "FloatingWindow"
    private var wm: WindowManager? = null
    private var rootView: View? = null
    private var infoText: TextView? = null
    private var isShown = false
    private var isRpcRecording = false
    private var currentActivity: String = ""
    private var captureFile: File? = null
    private var captureWriter: java.io.FileWriter? = null
    private val handler = Handler(Looper.getMainLooper())
    private var activityHookInstalled = false

    fun show(context: Context) {
        if (isShown) return
        val ctx = context.applicationContext
        wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // 安装 Activity 生命周期 Hook
        if (!activityHookInstalled) {
            installActivityHook()
            activityHookInstalled = true
        }

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#E8000000"))
            setPadding(10, 8, 10, 8)
        }

        // 标题栏
        val titleRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val icon = TextView(ctx).apply {
            text = "🔍"; textSize = 18f
        }
        val title = TextView(ctx).apply {
            text = " TK助手"
            setTextColor(Color.WHITE)
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        titleRow.addView(icon); titleRow.addView(title)
        root.addView(titleRow)

        // 信息区
        infoText = TextView(ctx).apply {
            setTextColor(Color.parseColor("#00FF00"))
            textSize = 9f
            text = "等待中..."
            setPadding(4, 4, 4, 4)
            layoutParams = LinearLayout.LayoutParams(-1, 120)
        }
        root.addView(infoText)

        // 按钮区
        fun makeBtn(label: String, color: Int, action: () -> Unit): Button {
            return Button(ctx).apply {
                text = label; textSize = 9f
                setBackgroundColor(color)
                setTextColor(Color.WHITE)
                setPadding(6, 2, 6, 2)
                setOnClickListener { action() }
            }
        }

        val btnRow1 = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        btnRow1.addView(makeBtn("📋页面", Color.parseColor("#1565C0")) { capturePage() })
        btnRow1.addView(makeBtn(if(isRpcRecording)"⏹停止" else "⏺录制", Color.parseColor("#C62828")) { toggleRpcRecord() })
        btnRow1.addView(makeBtn("▶️任务", Color.parseColor("#2E7D32")) { execMainTask() })
        btnRow1.addView(makeBtn("✕", Color.GRAY) { hide() })
        root.addView(btnRow1)

        rootView = root

        val params = WindowManager.LayoutParams(
            320, -2,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.END; x = 5; y = 150 }

        // 拖动
        var ix = 0; var iy = 0; var itx = 0f; var ity = 0f
        root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { ix = params.x; iy = params.y; itx = event.rawX; ity = event.rawY; true }
                MotionEvent.ACTION_MOVE -> {
                    params.x = ix + (event.rawX - itx).toInt()
                    params.y = iy + (event.rawY - ity).toInt()
                    wm?.updateViewLayout(rootView, params); true
                }
                MotionEvent.ACTION_UP -> { if(Math.abs(event.rawX-itx)<5&&Math.abs(event.rawY-ity)<5) root.performClick(); true }
                else -> false
            }
        }

        try {
            wm?.addView(rootView, params)
            isShown = true
            updateInfo("悬浮窗已就绪")
            Log.record(TAG, "悬浮窗已显示")
        } catch (e: Exception) {
            Log.record(TAG, "悬浮窗显示失败(可能缺少悬浮窗权限): ${e.message}")
            rootView = null
        }
    }

    fun hide() {
        try { if (rootView != null) wm?.removeView(rootView) } catch (_: Throwable) {}
        rootView = null; isShown = false
    }

    /** 捕获当前页面信息 */
    private fun capturePage() {
        val sb = StringBuilder()
        val ts = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        sb.appendLine("=== $ts ===")
        sb.appendLine("Activity: $currentActivity")
        // 获取 Fragment 信息
        try {
            val activity = getCurrentActivity()
            if (activity != null) {
                val fm = activity.fragmentManager
                val fragments = fm.fragments
                sb.appendLine("Fragments (${fragments.size}):")
                fragments.forEach { f ->
                    sb.appendLine("  ${f.javaClass.name}")
                    // 获取 View 层级
                    val view = f.view
                    if (view != null) {
                        dumpViewHierarchy(view, sb, "    ")
                    }
                }
            }
        } catch (_: Throwable) {}

        val info = sb.toString()
        updateInfo(info)

        // 追加到文件
        val file = File(Files.CONFIG_DIR.parentFile, "page_capture.txt")
        try {
            java.io.FileWriter(file, true).use { it.append(info + "\n") }
        } catch (_: Throwable) {}
        Log.record(TAG, "页面信息已捕获")
    }

    private fun dumpViewHierarchy(view: View, sb: StringBuilder, indent: String) {
        sb.appendLine("$indent${view.javaClass.simpleName} id=${view.id} ${view.width}x${view.height}")
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                dumpViewHierarchy(view.getChildAt(i), sb, "$indent  ")
                if (sb.length > 5000) break
            }
        }
    }

    /** RPC 录制开关 */
    private fun toggleRpcRecord() {
        isRpcRecording = !isRpcRecording
        if (isRpcRecording) {
            val fn = "rpc_cap_${SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())}.txt"
            captureFile = File(Files.CONFIG_DIR.parentFile, fn)
            try { captureWriter = java.io.FileWriter(captureFile, true) } catch (_: Throwable) {}
            updateInfo("🔴 录制中 → $fn")
        } else {
            try { captureWriter?.close() } catch (_: Throwable) {}
            captureWriter = null
            updateInfo("⚪ 已停止 (${captureFile?.length() ?: 0} bytes)")
        }
    }

    fun writeRpcEntry(entry: String) {
        try { captureWriter?.append(entry)?.append("\n")?.flush() } catch (_: Throwable) {}
    }

    /** 手动执行主任务 */
    private fun execMainTask() {
        try {
            ApplicationHook.execHandler()
            updateInfo("✅ 任务已触发")
        } catch (e: Exception) {
            updateInfo("❌ 触发失败: ${e.message}")
        }
    }

    private fun updateInfo(text: String) {
        handler.post {
            val sb = StringBuilder()
            if (isRpcRecording) sb.appendLine("🔴 录制中")
            if (currentActivity.isNotEmpty()) sb.appendLine("📱 $currentActivity")
            sb.append(text)
            infoText?.text = sb.toString()
        }
    }

    /** 获取当前 Activity */
    private fun getCurrentActivity(): Activity? {
        return try {
            val appClass = Class.forName("com.alipay.mobile.framework.AlipayApplication")
            val app = appClass.getMethod("getInstance").invoke(null)
            app?.javaClass?.getMethod("getMicroApplicationContext")?.invoke(app)
                ?.javaClass?.getMethod("getTopActivity")?.invoke(app?.javaClass
                    ?.getMethod("getMicroApplicationContext")?.invoke(app)) as? Activity
        } catch (_: Throwable) { null }
    }

    /** Hook Activity 生命周期来跟踪当前页面 */
    private fun installActivityHook() {
        try {
            XposedHelpers.findAndHookMethod(
                Activity::class.java, "onResume",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val activity = param.thisObject as Activity
                        currentActivity = activity.javaClass.simpleName
                        if (isShown) updateInfo("📱 $currentActivity")
                        // 自动捕获新 Activity
                        val entry = "[${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}] $currentActivity | ${activity.intent?.dataString ?: ""}"
                        Log.record("ActivityTrack", entry)
                        try { captureWriter?.append(entry)?.append("\n")?.flush() } catch (_: Throwable) {}
                    }
                })
            Log.record(TAG, "Activity Hook 安装成功")
        } catch (e: Throwable) {
            Log.record(TAG, "Activity Hook 失败: ${e.message}")
        }
    }
}
