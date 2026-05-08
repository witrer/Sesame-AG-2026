package fansirsqi.xposed.sesame.hook.internal

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import fansirsqi.xposed.sesame.hook.ApplicationHook
import fansirsqi.xposed.sesame.util.Files
import fansirsqi.xposed.sesame.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 悬浮窗调试助手 — 简洁圆球 + 展开面板
 */
object FloatingWindow {

    private const val TAG = "FloatingWindow"
    private var wm: WindowManager? = null
    private var ballView: View? = null
    private var panelView: View? = null
    private var isPanelShown = false
    private var isShown = false
    private var currentActivity = ""
    private var writer: FileWriter? = null
    private var isRecording = false

    private val ballSize = 88
    private val handler = Handler(Looper.getMainLooper())

    private val accentColor = Color.parseColor("#1677FF")
    private val bgColor = Color.parseColor("#E81A1A1A")

    private var params: WindowManager.LayoutParams? = null

    fun show(context: Context, processName: String? = null) {
        if (isShown) return
        if (processName != null && processName != "com.eg.android.AlipayGphone") {
            Log.record(TAG, "跳过: $processName"); return
        }
        val ctx = context.applicationContext
        wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        installActivityHook()

        // 悬浮球 — 圆角矩形
        ballView = TextView(ctx).apply {
            text = "TK"
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundColor(accentColor)
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE; cornerRadius = ballSize / 2f
                setColor(accentColor)
            }
            background = bg
            setPadding(0, 0, 0, 0)
        }

        params = WindowManager.LayoutParams(
            ballSize, ballSize,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 10; y = 300
        }

        var ix = 0; var iy = 0; var itx = 0f; var ity = 0f; var moved = false
        ballView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { ix = params!!.x; iy = params!!.y; itx = event.rawX; ity = event.rawY; moved = false; true }
                MotionEvent.ACTION_MOVE -> { params!!.x = ix + (event.rawX - itx).toInt(); params!!.y = iy + (event.rawY - ity).toInt(); wm?.updateViewLayout(ballView, params); moved = true; true }
                MotionEvent.ACTION_UP -> { if (!moved) togglePanel(ctx); true }
                else -> false
            }
        }

        wm?.addView(ballView, params)
        isShown = true
        Log.record(TAG, "悬浮球已显示")
    }

    private fun togglePanel(ctx: Context) {
        if (isPanelShown) { hidePanel(); return }
        showPanel(ctx)
    }

    private fun showPanel(ctx: Context) {
        hidePanel()
        val dp = ctx.resources.displayMetrics.density

        val panel = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
            setPadding(dp2px(12, dp), dp2px(10, dp), dp2px(12, dp), dp2px(10, dp))
            val bg = GradientDrawable().apply { shape = GradientDrawable.RECTANGLE; cornerRadius = dp2px(12, dp).toFloat(); setColor(bgColor) }
            background = bg
        }

        // 状态行
        val status = TextView(ctx).apply {
            text = "📍 $currentActivity"
            setTextColor(Color.parseColor("#AAAAAA")); textSize = 10f
            setPadding(0, 0, 0, dp2px(6, dp))
        }
        panel.addView(status)

        // 按钮
        fun addBtn(label: String, color: Int, action: () -> Unit): Button {
            return Button(ctx).apply {
                text = label
                setTextColor(Color.WHITE); textSize = 11f
                setBackgroundColor(color)
                setPadding(dp2px(10, dp), dp2px(6, dp), dp2px(10, dp), dp2px(6, dp))
                val bg = GradientDrawable().apply { shape = GradientDrawable.RECTANGLE; cornerRadius = dp2px(6, dp).toFloat(); setColor(color) }
                background = bg
                setOnClickListener { action() }
            }
        }

        val row1 = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        row1.addView(addBtn("📋 抓页面", Color.parseColor("#1565C0")) { capturePage() })
        row1.addView(Space(ctx).apply { layoutParams = LinearLayout.LayoutParams(dp2px(8, dp), 0) })
        row1.addView(addBtn(if(isRecording)"⏹ 停止" else "⏺ 录RPC", Color.parseColor("#C62828")) { toggleRecord() })
        panel.addView(row1)

        val row2 = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0, dp2px(6, dp), 0, 0) }
        row2.addView(addBtn("▶️ 跑任务", Color.parseColor("#2E7D32")) { ApplicationHook.execHandler() })
        row2.addView(Space(ctx).apply { layoutParams = LinearLayout.LayoutParams(dp2px(8, dp), 0) })
        row2.addView(addBtn("✕ 关闭", Color.GRAY) { hidePanel() })
        panel.addView(row2)

        panelView = panel

        val p = WindowManager.LayoutParams(-2, -2,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 10; y = 300 + ballSize + dp2px(8, dp)
        }

        wm?.addView(panelView, p)
        isPanelShown = true
    }

    private fun hidePanel() {
        try { if (panelView != null) wm?.removeView(panelView) } catch (_: Throwable) {}
        panelView = null; isPanelShown = false
    }

    private fun capturePage() {
        val sb = StringBuilder()
        val ts = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        sb.appendLine("=== $ts ===")
        sb.appendLine("Activity: $currentActivity")
        try {
            val app = Class.forName("com.alipay.mobile.framework.AlipayApplication")
            val inst = app.getMethod("getInstance").invoke(null)
            val mc = app.getMethod("getMicroApplicationContext").invoke(inst)
            val ta = mc.javaClass.getMethod("getTopActivity").invoke(mc) as? Activity
            if (ta != null) {
                val fm = ta.fragmentManager
                sb.appendLine("Fragments: ${fm.fragments.size}")
                fm.fragments.forEach { f -> sb.appendLine("  ${f.javaClass.name}") }
                // dump view tree
                if (ta.window?.decorView != null) {
                    dumpView(ta.window!!.decorView, sb, "  ", 0)
                }
            }
        } catch (_: Throwable) {}
        val info = sb.toString()
        // Save
        val file = File(Files.CONFIG_DIR.parentFile, "page_capture.txt")
        try { FileWriter(file, true).use { it.append(info + "\n") } } catch (_: Throwable) {}
        Log.record(TAG, "页面已捕获")
        hidePanel()
    }

    private fun dumpView(v: View, sb: StringBuilder, indent: String, depth: Int) {
        if (depth > 15 || sb.length > 3000) return
        val idName = try { v.resources.getResourceEntryName(v.id) } catch (_: Throwable) { "0x${Integer.toHexString(v.id)}" }
        sb.appendLine("$indent${v.javaClass.simpleName} [$idName] ${v.width}x${v.height}")
        if (v is ViewGroup) {
            for (i in 0 until v.childCount.coerceAtMost(10)) {
                dumpView(v.getChildAt(i), sb, "$indent  ", depth + 1)
            }
        }
    }

    private fun toggleRecord() {
        isRecording = !isRecording
        if (isRecording) {
            val fn = "rpc_fw_${SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())}.txt"
            writer = FileWriter(File(Files.CONFIG_DIR.parentFile, fn), true)
        } else {
            try { writer?.close() } catch (_: Throwable) {}
            writer = null
        }
    }

    fun writeRpc(entry: String) {
        try { writer?.append(entry)?.append("\n")?.flush() } catch (_: Throwable) {}
    }

    private fun installActivityHook() {
        try {
            XposedHelpers.findAndHookMethod(Activity::class.java, "onResume", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    currentActivity = (param.thisObject as Activity).javaClass.simpleName
                    try { writer?.append("[${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}] $currentActivity\n")?.flush() } catch (_: Throwable) {}
                }
            })
        } catch (_: Throwable) {}
    }

    private fun dp2px(dp: Int, density: Float) = (dp * density + 0.5f).toInt()
}
