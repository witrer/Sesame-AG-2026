package fansirsqi.xposed.sesame.util

import java.io.File

data class ModuleRuntimeStatus(
    val framework: String,
    val timestamp: Long,
    val packageName: String
)

object StatusManager {
    private const val TAG = "StatusManager"
    private const val STATUS_FILE_NAME = "ModuleStatus.json"

    private fun getStatusFile(): File {
        return File(Files.CONFIG_DIR.parentFile, STATUS_FILE_NAME)
    }

    /** [Hook端] 写入当前激活状态 */
    fun updateStatus(framework: String, packageName: String) {
        try {
            // 写入文件
            val status = ModuleRuntimeStatus(framework, System.currentTimeMillis(), packageName)
            val json = JsonHelper.toJson(status)
            Files.write2File(json, getStatusFile())

            // 同时写入 XSharedPreferences 供模块 App 读取
            try {
                val prefs = de.robv.android.xposed.XSharedPreferences(
                    fansirsqi.xposed.sesame.data.General.MODULE_PACKAGE_NAME,
                    fansirsqi.xposed.sesame.SesameApplication.PREFERENCES_KEY
                )
                prefs.makeWorldReadable()
                prefs.reload()
                prefs.edit()
                    .putString("status_framework", framework)
                    .putLong("status_timestamp", System.currentTimeMillis())
                    .putString("status_package", packageName)
                    .apply()
            } catch (_: Throwable) {}

            Log.d(TAG, "Status updated: $framework")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write status", e)
        }
    }

    /** [UI端] 读取激活状态 */
    fun readStatus(): ModuleRuntimeStatus? {
        // 优先从文件读取（可靠性高）
        try {
            val file = getStatusFile()
            if (file.exists()) {
                val json = Files.readFromFile(file)
                val status = JsonHelper.fromJson<ModuleRuntimeStatus>(json)
                if (status != null && status.framework.isNotEmpty()) {
                    return status
                }
            }
        } catch (_: Exception) {}

        // 备用：从 XSharedPreferences 读取
        try {
            val prefs = de.robv.android.xposed.XSharedPreferences(
                fansirsqi.xposed.sesame.data.General.MODULE_PACKAGE_NAME,
                fansirsqi.xposed.sesame.SesameApplication.PREFERENCES_KEY
            )
            prefs.makeWorldReadable()
            prefs.reload()
            val framework = prefs.getString("status_framework", null)
            if (!framework.isNullOrEmpty()) {
                return ModuleRuntimeStatus(
                    framework = framework,
                    timestamp = prefs.getLong("status_timestamp", 0L),
                    packageName = prefs.getString("status_package", "") ?: ""
                )
            }
        } catch (_: Throwable) {}

        return null
    }
}
