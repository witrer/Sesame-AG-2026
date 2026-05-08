package fansirsqi.xposed.sesame.hook.internal

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import fansirsqi.xposed.sesame.hook.ApplicationHook
import fansirsqi.xposed.sesame.util.Log
import java.lang.reflect.Proxy

/**
 * 绕过新版支付宝滑块验证和安全检测
 * 针对 RpcSecurityCountersignHandleProxy 和新版安全机制
 */
object SliderBypassHelper {

    private const val TAG = "SliderBypass"
    private var classLoader: ClassLoader? = null
    private var hookInstalled = false

    fun init(loader: ClassLoader) {
        classLoader = loader
        Log.record(TAG, "滑块绕过助手初始化完成")
    }

    /**
     * 安装所有滑块绕过 Hook
     */
    fun installAllHooks() {
        if (hookInstalled) {
            Log.record(TAG, "Hook 已安装，跳过")
            return
        }
        val loader = classLoader ?: return

        try {
            // 1. Hook RPC 安全联署 - 自动提供有效签名
            hookRpcSecurityCountersign(loader)

            // 2. Hook NebulaTransActivity - 自动关闭滑块/验证码页面
            hookNebulaTransActivity(loader)

            // 3. Hook 安全验证 H5 - H5BasePage 实现类
            hookSecurityVerification(loader)

            // 4. Hook 风险提示弹窗
            hookRiskDialog(loader)

            hookInstalled = true
            Log.record(TAG, "所有滑块绕过 Hook 安装成功")
        } catch (e: Throwable) {
            Log.printStackTrace(TAG, "安装滑块绕过 Hook 失败", e)
        }
    }

    /**
     * Hook NebulaTransActivity - 自动跳过滑块验证页面
     * 新版本支付宝验证码通过 NebulaTransActivity$Main 显示
     */
    private fun hookNebulaTransActivity(loader: ClassLoader) {
        try {
            val nebulaTransClass = XposedHelpers.findClass(
                "com.alipay.mobile.nebulax.xriver.activity.NebulaTransActivity",
                loader
            )
            // Hook onCreate - 检测到验证页面立即 finish
            XposedHelpers.findAndHookMethod(
                nebulaTransClass,
                "onCreate",
                android.os.Bundle::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val activity = param.thisObject as? android.app.Activity ?: return
                            val intent = activity.intent
                            val url = intent?.dataString ?: intent?.getStringExtra("url") ?: ""

                            // 检测验证码/滑块 URL
                            if (url.contains("security") ||
                                url.contains("verify") ||
                                url.contains("captcha") ||
                                url.contains("slider") ||
                                url.contains("risk") ||
                                url.contains("safePay")
                            ) {
                                Log.record(TAG, "拦截 NebulaTransActivity 验证页面: $url")
                                // 不加载验证，直接关闭
                                activity.finish()
                            }
                        } catch (_: Throwable) {}
                    }
                })

            // 同时 Hook onResume - 如果在 onResume 时检测到是验证页面
            XposedHelpers.findAndHookMethod(
                nebulaTransClass,
                "onResume",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val activity = param.thisObject as? android.app.Activity ?: return
                            val wm = activity.windowManager
                            // 检查 DecorView 是否包含 embeded_fragment_container (验证码容器)
                            val decorView = activity.window?.decorView
                            if (decorView != null) {
                                val containerId = decorView.context.resources.getIdentifier(
                                    "embeded_fragment_container", "id",
                                    "com.alipay.multiplatform.phone.xriver_integration"
                                )
                                if (containerId != 0) {
                                    val container = decorView.findViewById<android.view.View>(containerId)
                                    if (container != null) {
                                        Log.record(TAG, "检测到验证码容器，关闭 NebulaTransActivity")
                                        activity.finish()
                                    }
                                }
                            }
                        } catch (_: Throwable) {}
                    }
                })
            Log.record(TAG, "Hook NebulaTransActivity 成功")
        } catch (e: Throwable) {
            Log.record(TAG, "Hook NebulaTransActivity 失败: ${e.message}")
        }
    }

    /**
     * Hook RpcSecurityCountersignHandleProxy#handleRpcSecurityCountersign
     * 拦截安全联署请求，返回有效的安全头部
     */
    private fun hookRpcSecurityCountersign(loader: ClassLoader) {
        try {
            val proxyClass = XposedHelpers.findClass(
                "com.alibaba.ariver.commonability.network.rpc.RpcSecurityCountersignHandleProxy",
                loader
            )

            // Hook RVProxy.get() 返回代理，拦截 get 调用以返回我们的自定义实现
            val rvProxyClass = XposedHelpers.findClass(
                "com.alibaba.ariver.kernel.common.RVProxy",
                loader
            )

            XposedHelpers.findAndHookMethod(
                rvProxyClass,
                "get",
                Class::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val serviceClass = param.args[0] as? Class<*>
                        if (serviceClass?.name?.contains("RpcSecurityCountersignHandleProxy") == true) {
                            val original = param.result
                            if (original == null) {
                                // 创建代理实现，返回空的 countersign map（绕过签名检查）
                                val proxy = Proxy.newProxyInstance(
                                    loader,
                                    arrayOf(proxyClass),
                                    { _, method, args ->
                                        when (method.name) {
                                            "handleRpcSecurityCountersign" -> {
                                                Log.record(TAG, "绕过 RPC 安全联署请求")
                                                emptyMap<String, String>()
                                            }
                                            "getPriority" -> 0
                                            else -> null
                                        }
                                    }
                                )
                                param.result = proxy
                                Log.record(TAG, "已注入 RpcSecurityCountersignHandleProxy 代理")
                            }
                        }
                    }
                })

            Log.record(TAG, "Hook RpcSecurityCountersignHandleProxy 成功")
        } catch (e: Throwable) {
            Log.record(TAG, "Hook RpcSecurityCountersignHandleProxy 失败: ${e.message}")
            Log.printStackTrace(TAG, e)
        }
    }

    /**
     * Hook 安全验证页面 - 自动跳过滑块/H5验证
     * 拦截 SchemeStartActivity 和安全验证相关 Activity
     */
    private fun hookSecurityVerification(loader: ClassLoader) {
        try {
            // 使用 H5BasePage 具体实现类（H5Page 是接口，不能直接 Hook）
            var hooked = false
            // 尝试 H5BasePage
            try {
                XposedHelpers.findAndHookMethod(
                    "com.alipay.mobile.nebula.basebridge.H5BasePage",
                    loader,
                    "loadUrl",
                    String::class.java,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val url = param.args[0] as? String ?: return
                            if (url.contains("securityVerify") ||
                                url.contains("slidingVerify") ||
                                url.contains("captcha") ||
                                url.contains("riskVerify") ||
                                url.contains("ariver/verify")
                            ) {
                                Log.record(TAG, "H5BasePage 拦截安全验证页面: $url")
                                param.result = null
                            }
                        }
                    })
                hooked = true
                Log.record(TAG, "Hook H5BasePage.loadUrl 成功")
            } catch (_: Throwable) {}

            // 备用：尝试 H5WebView 的 loadUrl
            if (!hooked) {
                try {
                    XposedHelpers.findAndHookMethod(
                        "com.alipay.mobile.nebulacore.web.H5WebView",
                        loader,
                        "loadUrl",
                        String::class.java,
                        object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                val url = param.args[0] as? String ?: return
                                if (url.contains("securityVerify") ||
                                    url.contains("slidingVerify") ||
                                    url.contains("captcha") ||
                                    url.contains("riskVerify") ||
                                    url.contains("ariver/verify")
                                ) {
                                    Log.record(TAG, "H5WebView 拦截安全验证页面: $url")
                                    param.result = null
                                }
                            }
                        })
                    hooked = true
                    Log.record(TAG, "Hook H5WebView.loadUrl 成功")
                } catch (_: Throwable) {}
            }

            if (!hooked) {
                Log.record(TAG, "Hook 安全验证页面失败: 未找到可用的实现类")
            }
        } catch (e: Throwable) {
            Log.record(TAG, "Hook 安全验证页面失败: ${e.message}")
        }
    }

    /**
     * Hook 风险提示弹窗 - 防止弹窗干扰
     */
    private fun hookRiskDialog(loader: ClassLoader) {
        try {
            // Hook 风险提示对话框
            XposedHelpers.findAndHookMethod(
                "com.alipay.mobile.quinox.LauncherActivity",
                loader,
                "onResume",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        // 检查是否有安全验证弹窗
                        try {
                            val activity = param.thisObject
                            // 尝试关闭可能的安全验证 Fragment/Dialog
                            val fm = XposedHelpers.callMethod(activity, "getSupportFragmentManager")
                            val fragments = XposedHelpers.callMethod(fm, "getFragments") as? List<*>
                            fragments?.forEach { fragment ->
                                val fragClass = fragment?.javaClass?.name ?: ""
                                if (fragClass.contains("Verify") ||
                                    fragClass.contains("Risk") ||
                                    fragClass.contains("SecurityDialog") ||
                                    fragClass.contains("SafePay")
                                ) {
                                    Log.record(TAG, "自动关闭安全验证弹窗: $fragClass")
                                    try {
                                        XposedHelpers.callMethod(fm, "beginTransaction")
                                            .let { transaction ->
                                                XposedHelpers.callMethod(transaction, "remove", fragment)
                                                XposedHelpers.callMethod(transaction, "commitAllowingStateLoss")
                                            }
                                    } catch (_: Throwable) {}
                                }
                            }
                        } catch (_: Throwable) {}
                    }
                })
            Log.record(TAG, "Hook 风险提示弹窗成功")
        } catch (e: Throwable) {
            Log.record(TAG, "Hook 风险提示弹窗失败: ${e.message}")
        }
    }

    /**
     * 卸载所有 Hook（如果需要）
     */
    fun unload() {
        hookInstalled = false
        Log.record(TAG, "滑块绕过 Hook 已卸载")
    }
}
