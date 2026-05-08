package fansirsqi.xposed.sesame.task.browseVideo

import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.model.ModelGroup
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.RandomUtil
import fansirsqi.xposed.sesame.util.ResChecker
import kotlinx.coroutines.delay
import org.json.JSONObject

/**
 * 首页视频红包任务 - 看视频领红包
 * 需要先抓包确定正确的RPC接口。当前使用 AntFarm 已验证的视频流程作为基础。
 */
class BrowseVideo : ModelTask() {

    companion object {
        private const val TAG = "BrowseVideo"
        const val MODULE_NAME = "视频红包"
        private const val VERSION = "0.1.2601161444.47"

        @Volatile
        var instance: BrowseVideo? = null
            private set
    }

    private lateinit var videoAutoBrowse: BooleanModelField
    private lateinit var videoMaxCount: IntegerModelField
    private lateinit var videoBrowseDuration: IntegerModelField

    override fun getName(): String = MODULE_NAME
    override fun getGroup(): ModelGroup = ModelGroup.OTHER
    override fun getIcon(): String = "Default.png"

    override fun getFields(): ModelFields {
        return ModelFields().apply {
            addField(BooleanModelField("videoAutoBrowse", "视频红包 | 自动浏览", true).also {
                videoAutoBrowse = it
            })
            addField(IntegerModelField("videoMaxCount", "视频红包 | 每日最大次数", 5, 1, 30).also {
                videoMaxCount = it
            })
            addField(IntegerModelField("videoBrowseDuration", "视频红包 | 模拟浏览时长(秒)", 15, 5, 30).also {
                videoBrowseDuration = it
            })
        }
    }

    override fun prepare() { instance = this }
    override fun boot(clazz: ClassLoader?) { super.boot(clazz) }
    override fun destroy() { instance = null; super.destroy() }

    override suspend fun runSuspend() {
        if (!videoAutoBrowse.value) {
            Log.record(TAG, "视频红包自动浏览未开启")
            return
        }

        val maxCount = videoMaxCount.value
        val browseDuration = videoBrowseDuration.value
        Log.record(TAG, "开始浏览视频任务，目标: $maxCount 次，时长: ${browseDuration}秒")

        var success = 0
        var fail = 0

        // 获取所有任务
        val taskResult = BrowseVideoRpcCall.listFarmTask(VERSION)
        val tasks = try {
            JSONObject(taskResult).optJSONArray("taskInfoList")
        } catch (_: Exception) { null }

        if (tasks == null || tasks.length() == 0) {
            Log.record(TAG, "未找到任何任务 (listFarmTask 返回空)")
            return
        }

        Log.record(TAG, "共 ${tasks.length()} 个任务，筛选视频类...")

        for (i in 0 until tasks.length()) {
            if (success >= maxCount) break
            try {
                val task = tasks.getJSONObject(i)
                val bizKey = task.optString("bizKey", "")
                val title = task.optString("title", bizKey)

                // 只处理视频/BROWSE类任务
                val isVideoTask = bizKey.contains("VIDEO", ignoreCase = true) ||
                        title.contains("视频") ||
                        bizKey.contains("BROWSE", ignoreCase = true)
                if (!isVideoTask) continue

                delay(RandomUtil.nextLong(2000, 5000))
                Log.record(TAG, "[${success + 1}/$maxCount] $title ($bizKey)")

                // 执行任务获取视频URL
                val doResult = BrowseVideoRpcCall.doFarmTask(bizKey, VERSION)
                val doJson = try { JSONObject(doResult) } catch (_: Exception) { null }
                val videoUrl = doJson?.optString("videoUrl", "")
                        ?: doJson?.optJSONObject("extendInfo")?.optString("videoUrl", "")
                        ?: ""

                if (videoUrl.isNotEmpty()) {
                    // 提取 contentId
                    val contentId = extractContentId(videoUrl)
                    if (contentId.isNotEmpty()) {
                        // 下发+观看+触发奖励（AntFarm 已验证流程）
                        if (ResChecker.checkRes(TAG, JSONObject(BrowseVideoRpcCall.videoDeliverModule(contentId)))) {
                            delay(browseDuration * 1000L)
                            if (ResChecker.checkRes(TAG, JSONObject(BrowseVideoRpcCall.videoTrigger(contentId)))) {
                                success++
                                Log.other(TAG, "视频红包完成🧧[$title]")
                                continue
                            }
                        }
                    }
                }

                // 非视频URL的任务，尝试直接 doFarmTask 完成
                if (doJson != null && doJson.optBoolean("success", false)) {
                    success++
                    Log.other(TAG, "任务完成✅[$title]")
                } else {
                    fail++
                    Log.record(TAG, "任务失败[$title]")
                }

            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                fail++
                Log.record(TAG, "异常: ${e.message}")
            }
        }

        Log.record(TAG, "视频红包完成！成功: $success, 失败: $fail")
    }

    private fun extractContentId(url: String): String {
        return try {
            val idx = url.indexOf("&contentId=")
            if (idx < 0) return ""
            val start = idx + 11
            val end = url.indexOf("&", start)
            if (end > start) url.substring(start, end) else url.substring(start)
        } catch (_: Exception) { "" }
    }
}
