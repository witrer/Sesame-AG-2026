package fansirsqi.xposed.sesame.hook.lsp100

import de.robv.android.xposed.XposedBridge
import fansirsqi.xposed.sesame.data.General
import fansirsqi.xposed.sesame.hook.ApplicationHook
import fansirsqi.xposed.sesame.hook.XposedEnv
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface

class HookEntry(
    base: XposedInterface, param: XposedModuleInterface.ModuleLoadedParam
) : XposedModule(base, param) {
    val tag = "LsposedEntry"
    private val processName = param.processName
    var customHooker: ApplicationHook? = null


    init {
        customHooker = ApplicationHook()
        customHooker?.xposedInterface = base
        // 将框架提供的 base 接口实例传递给逻辑核心，连接 Hook 进程与框架功能。
        XposedBridge.log("$tag: Initialized for process $processName")


        val baseFw = "${base.frameworkName} ${base.frameworkVersion} ${base.frameworkVersionCode} target_model_process: ${base.applicationInfo.processName}"
        XposedBridge.log("LspEntry: Framework from base: $baseFw ")
    }

    /**
     * 当模块作用域内的应用进程启动时，框架会回调此方法。
     */
    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        try {
            if (General.PACKAGE_NAME != param.packageName) return
            XposedEnv.classLoader = param.classLoader
            XposedEnv.appInfo = param.applicationInfo
            XposedEnv.packageName = param.packageName
            XposedEnv.processName = processName
            customHooker?.loadPackage(param)
            XposedBridge.log("$tag: Hooking ${param.packageName} in process $processName")
        } catch (e: Throwable) {
            XposedBridge.log("$tag: Hook failed - ${e.message}")
            XposedBridge.log(e)
        }
    }
}