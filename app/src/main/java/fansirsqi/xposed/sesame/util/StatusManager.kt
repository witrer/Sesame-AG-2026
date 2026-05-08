package fansirsqi.xposed.sesame.util

import de.robv.android.xposed.XSharedPreferences
import fansirsqi.xposed.sesame.SesameApplication

data class ModuleRuntimeStatus(
    val framework: String,
    val timestamp: Long,
    val packageName: String
)

object StatusManager {
    private const val TAG = "StatusManager"
    private const val KEY_FRAMEWORK = "status_framework"
    private const val KEY_TIMESTAMP = "status_timestamp"
    private const val KEY_PACKAGE = "status_package"

    /** 获取 XSharedPreferences，用于跨进程共享状态 */
    private fun getPrefs(): XSharedPreferences {
        val prefs = XSharedPreferences(
            fansirsqi.xposed.sesame.data.General.MODULE_PACKAGE_NAME,
            SesameApplication.PREFERENCES_KEY
        )
        prefs.makeWorldReadable()
        return prefs
    }

    /** [Hook端] 写入当前激活状态到 XSharedPreferences */
    fun updateStatus(framework: String, packageName: String) {
        try {
            val prefs = getPrefs()
            prefs.reload()
            val editor = prefs.edit()
            editor.putString(KEY_FRAMEWORK, framework)
            editor.putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            editor.putString(KEY_PACKAGE, packageName)
            editor.apply()

            // 同时写文件作为备份
            try {
                val status = ModuleRuntimeStatus(framework, System.currentTimeMillis(), packageName)
                val json = JsonHelper.toJson(status)
                Files.write2File(json, java.io.File(Files.CONFIG_DIR.parentFile, "ModuleStatus.json"))
            } catch (_: Exception) {}

            Log.d(TAG, "Status updated via XSharedPreferences: $framework")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write status", e)
        }
    }

    /** [UI端] 读取激活状态 */
    fun readStatus(): ModuleRuntimeStatus? {
        return try {
            val prefs = getPrefs()
            prefs.reload()
            val framework = prefs.getString(KEY_FRAMEWORK, null)
            if (framework.isNullOrEmpty()) return null
            val timestamp = prefs.getLong(KEY_TIMESTAMP, 0L)
            val packageName = prefs.getString(KEY_PACKAGE, "") ?: ""
            ModuleRuntimeStatus(framework, timestamp, packageName)
        } catch (e: Exception) {
            null
        }
    }
}
