package fansirsqi.xposed.sesame.hook.internal

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
 * RPC 数据抓包助手
 * 通过广播触发录制：com.eg.android.AlipayGphone.sesame.capture_start / capture_stop
 * 录制文件保存在 sesame-TK 目录下 rpc_capture_*.txt
 */
object RpcCaptureHelper {

    private const val TAG = "RpcCapture"
    private const val BROADCAST_START = "com.eg.android.AlipayGphone.sesame.capture_start"
    private const val BROADCAST_STOP = "com.eg.android.AlipayGphone.sesame.capture_stop"

    private var classLoader: ClassLoader? = null
    @Volatile var isRecording = false
        private set
    private val capturedRpcs = mutableListOf<String>()
    private lateinit var captureFile: File
    private var hookInstalled = false
    private val triggerFile: File by lazy {
        File(Files.CONFIG_DIR.parentFile, "capture_trigger")
    }

    fun init(loader: ClassLoader) {
        classLoader = loader
        // 自动开始录制（通过 capture_trigger 文件控制停止）
        startRecording()
    }

    fun startRecording() {
        if (isRecording) return
        isRecording = true
        captureFile = File(Files.CONFIG_DIR.parentFile,
            "rpc_capture_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.txt")
        capturedRpcs.clear()
        Log.record(TAG, "🔴 开始录制 RPC → ${captureFile.name}")
    }

    fun stopRecording() {
        if (!isRecording) return
        isRecording = false
        try {
            val content = capturedRpcs.joinToString("\n")
            Files.write2File(content, captureFile)
            Log.record(TAG, "⚪ 停止录制，${capturedRpcs.size} 条记录 → ${captureFile.name}")
        } catch (e: Exception) {
            Log.error(TAG, "保存失败: ${e.message}")
        }
        capturedRpcs.clear()
    }

    /** 安装 RPC 拦截 Hook */
    fun installRpcCaptureHooks() {
        if (hookInstalled) return
        val loader = classLoader ?: return
        try {
            val bridgeClass = XposedHelpers.findClass(
                "com.alibaba.ariver.commonability.network.rpc.RpcBridgeExtension", loader
            )
            val jsonClass = Class.forName("com.alibaba.fastjson.JSONObject", false, loader)

            XposedHelpers.findAndHookMethod(
                bridgeClass, "rpc",
                String::class.java, java.lang.Boolean.TYPE, java.lang.Boolean.TYPE,
                String::class.java, jsonClass, String::class.java, jsonClass,
                java.lang.Boolean.TYPE, java.lang.Boolean.TYPE,
                Integer.TYPE, java.lang.Boolean.TYPE, String::class.java,
                XposedHelpers.findClass("com.alibaba.ariver.app.api.App", loader),
                XposedHelpers.findClass("com.alibaba.ariver.app.api.Page", loader),
                XposedHelpers.findClass("com.alibaba.ariver.engine.api.bridge.model.ApiContext", loader),
                XposedHelpers.findClass("com.alibaba.ariver.engine.api.bridge.extension.BridgeCallback", loader),
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        checkTriggerFile()  // 每次RPC调用前检查触发文件
                        if (!isRecording) return
                        try {
                            val method = param.args[0] as? String ?: return
                            val params = param.args[4]
                            val data = if (params != null) params.toString() else "null"
                            val ts = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
                            addEntry("[$ts] REQ $method\n  $data")
                        } catch (_: Throwable) {}
                    }
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!isRecording) return
                        try {
                            val method = param.args[0] as? String ?: return
                            val cb = param.args[15] ?: return
                            val respField = cb.javaClass.getDeclaredField("mJSONResponse")
                            respField.isAccessible = true
                            val resp = respField.get(cb)
                            if (resp != null) {
                                val ts = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
                                val data = resp.toString()
                                val short = if (data.length > 800) data.take(800) + "..." else data
                                addEntry("[$ts] RES $method\n  $short")
                            }
                        } catch (_: Throwable) {}
                    }
                })
            hookInstalled = true
            Log.record(TAG, "RPC 抓包 Hook 安装成功")
        } catch (e: Throwable) {
            Log.record(TAG, "RPC 抓包 Hook 安装失败: ${e.message}")
        }
    }

    private fun addEntry(entry: String) {
        synchronized(capturedRpcs) {
            capturedRpcs.add(entry)
            if (capturedRpcs.size > 500) capturedRpcs.removeAt(0)
        }
    }

    /** 处理广播命令 */
    fun handleBroadcast(action: String?) {
        when (action) {
            BROADCAST_START -> startRecording()
            BROADCAST_STOP -> stopRecording()
        }
    }
}
