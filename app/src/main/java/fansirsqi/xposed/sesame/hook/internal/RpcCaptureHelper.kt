package fansirsqi.xposed.sesame.hook.internal

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import fansirsqi.xposed.sesame.util.Files
import fansirsqi.xposed.sesame.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RPC 数据抓包助手 - 同时 Hook 新旧 RPC 通道，实时写入文件
 */
object RpcCaptureHelper {

    private const val TAG = "RpcCapture"
    private var classLoader: ClassLoader? = null
    @Volatile var isRecording = false
        private set
    private var writer: FileWriter? = null
    private var hookInstalled = false

    fun init(loader: ClassLoader) {
        classLoader = loader
        startRecording()
    }

    @Synchronized
    fun startRecording() {
        if (isRecording) return
        isRecording = true
        val dir = android.os.Environment.getExternalStorageDirectory().toString() +
                "/Android/media/com.eg.android.AlipayGphone/sesame-TK"
        val file = File(dir, "rpc_cap_${SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())}.txt")
        try {
            if (!file.parentFile!!.exists()) file.parentFile!!.mkdirs()
            writer = FileWriter(file, true)
            Log.record(TAG, "🔴 录制 → ${file.absolutePath}")
            write("=== START ===\n")
        } catch (e: Exception) {
            Log.error(TAG, "创建文件失败: ${e.message}")
            Log.printStackTrace(TAG, e)
        }
    }

    @Synchronized
    fun stopRecording() {
        if (!isRecording) return
        isRecording = false
        try {
            write("=== RPC Capture Stopped ===\n")
            writer?.flush()
            writer?.close()
        } catch (_: Throwable) {}
        writer = null
        Log.record(TAG, "⚪ 停止录制")
    }

    @Synchronized
    private fun write(text: String) {
        try {
            writer?.append(text)?.flush()
        } catch (_: Throwable) {}
    }

    /** 安装 RPC 拦截 Hook - 同时覆盖新旧两条 RPC 通道 */
    fun installRpcCaptureHooks() {
        if (hookInstalled) return
        val loader = classLoader ?: return

        // 1. 新版 RPC: RpcBridgeExtension.rpc()
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
                createNewRpcHook()
            )
            Log.record(TAG, "新RPC Hook 安装成功")
        } catch (e: Throwable) {
            Log.record(TAG, "新RPC Hook 失败: ${e.message}")
        }

        // 2. 旧版 RPC: H5RpcUtil.rpcCall()
        try {
            val h5RpcUtilClass = XposedHelpers.findClass(
                "com.alipay.mobile.nebulaappproxy.api.rpc.H5RpcUtil", loader
            )
            val h5PageClass = XposedHelpers.findClass(
                "com.alipay.mobile.h5container.api.H5Page", loader
            )
            XposedHelpers.findAndHookMethod(
                h5RpcUtilClass, "rpcCall",
                String::class.java, String::class.java, String::class.java,
                java.lang.Boolean.TYPE,
                loader.loadClass("com.alibaba.fastjson.JSONObject"),
                String::class.java, java.lang.Boolean.TYPE,
                h5PageClass, Integer.TYPE,
                String::class.java, java.lang.Boolean.TYPE,
                Integer.TYPE, String::class.java,
                createOldRpcHook()
            )
            Log.record(TAG, "旧RPC Hook 安装成功")
        } catch (e: Throwable) {
            Log.record(TAG, "旧RPC Hook 失败: ${e.message}")
        }

        hookInstalled = true
    }

    private fun createNewRpcHook() = object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            if (!isRecording || writer == null) return
            try {
                val method = param.args[0] as? String ?: return
                val params = param.args[4]?.toString() ?: "null"
                val ts = now()
                write("[$ts] NEW_REQ $method\n  $params\n")
            } catch (_: Throwable) {}
        }
        override fun afterHookedMethod(param: MethodHookParam) {
            if (!isRecording || writer == null) return
            try {
                val method = param.args[0] as? String ?: return
                val cb = param.args[15] ?: return
                val respField = cb.javaClass.getDeclaredField("mJSONResponse")
                respField.isAccessible = true
                val resp = respField.get(cb)?.toString()
                if (resp != null) {
                    val short = if (resp.length > 1000) resp.take(1000) + "..." else resp
                    write("[${now()}] NEW_RES $method\n  $short\n")
                }
            } catch (_: Throwable) {}
        }
    }

    private fun createOldRpcHook() = object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            if (!isRecording || writer == null) return
            try {
                val method = param.args[0] as? String ?: return
                val args = param.args[1]?.toString() ?: "null"
                write("[${now()}] OLD_REQ $method\n  $args\n")
            } catch (_: Throwable) {}
        }
        override fun afterHookedMethod(param: MethodHookParam) {
            if (!isRecording || writer == null) return
            try {
                val method = param.args[0] as? String ?: return
                val result = param.result ?: return
                val resp = result.javaClass.getMethod("getResponse").invoke(result) as? String ?: return
                val short = if (resp.length > 1000) resp.take(1000) + "..." else resp
                write("[${now()}] OLD_RES $method\n  $short\n")
            } catch (_: Throwable) {}
        }
    }

    private fun now() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
}
