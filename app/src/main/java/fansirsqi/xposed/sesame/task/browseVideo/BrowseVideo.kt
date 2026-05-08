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
 * 首页视频红包 — 看视频领现金
 * 优先使用 AntFarm 已验证流程，fallback 尝试其他
 */
class BrowseVideo : ModelTask() {

    companion object {
        private const val TAG = "BrowseVideo"
        const val MODULE_NAME = "视频红包"
        private const val VERSION = "0.1.2601161444.47"

        @Volatile var instance: BrowseVideo? = null
    }

    private lateinit var videoAutoBrowse: BooleanModelField
    private lateinit var videoMaxCount: IntegerModelField
    private lateinit var videoBrowseDuration: IntegerModelField
    private lateinit var videoAutoSignIn: BooleanModelField

    override fun getName() = MODULE_NAME
    override fun getGroup() = ModelGroup.OTHER
    override fun getIcon() = "Default.png"

    override fun getFields() = ModelFields().apply {
        addField(BooleanModelField("videoAutoBrowse", "视频红包 | 自动浏览", true).also { videoAutoBrowse = it })
        addField(IntegerModelField("videoMaxCount", "视频红包 | 每日最大次数", 10, 1, 50).also { videoMaxCount = it })
        addField(IntegerModelField("videoBrowseDuration", "视频红包 | 模拟浏览时长(秒)", 15, 10, 60).also { videoBrowseDuration = it })
        addField(BooleanModelField("videoAutoSignIn", "视频红包 | 顺便签到", true).also { videoAutoSignIn = it })
    }

    override fun prepare() { instance = this }
    override fun boot(clazz: ClassLoader?) { super.boot(clazz) }
    override fun destroy() { instance = null; super.destroy() }

    override suspend fun runSuspend() {
        if (!videoAutoBrowse.value) return

        val maxCount = videoMaxCount.value
        val duration = videoBrowseDuration.value
        Log.record(TAG, "开始视频红包任务，目标 $maxCount 次，时长 ${duration}秒")

        var success = 0; var fail = 0

        // 方案A: 通过 AntFarm listFarmTask + doFarmTask 流程（已验证可行）
        val farmResult = BrowseVideoRpcCall.listFarmTask(VERSION)
        val farmTasks = try { JSONObject(farmResult).optJSONArray("taskInfoList") } catch (_: Exception) { null }

        if (farmTasks != null && farmTasks.length() > 0) {
            Log.record(TAG, "方案A: AntFarm 任务 ${farmTasks.length()} 个")
            for (i in 0 until farmTasks.length()) {
                if (success >= maxCount) break
                try {
                    val t = farmTasks.getJSONObject(i)
                    val bizKey = t.optString("bizKey", "")
                    val title = t.optString("title", bizKey)
                    // 只处理视频类
                    if (!bizKey.contains("VIDEO", true) && !title.contains("视频")) continue
                    delay(RandomUtil.nextLong(2000, 5000))
                    Log.record(TAG, "[${success + 1}] $title")

                    val doResult = BrowseVideoRpcCall.doFarmTask(bizKey, VERSION)
                    val doJson = try { JSONObject(doResult) } catch (_: Exception) { null }
                    val videoUrl = doJson?.optString("videoUrl", "") ?: ""
                    if (videoUrl.isNotEmpty()) {
                        val contentId = extractContentId(videoUrl)
                        if (contentId.isNotEmpty() && ResChecker.checkRes(TAG, JSONObject(BrowseVideoRpcCall.videoDeliverModule(contentId)))) {
                            delay(duration * 1000L)
                            if (ResChecker.checkRes(TAG, JSONObject(BrowseVideoRpcCall.videoTrigger(contentId)))) {
                                success++; Log.other(TAG, "视频完成🧧[$title]"); continue
                            }
                        }
                    }
                    fail++; Log.record(TAG, "方案A失败[$title]")
                } catch (_: kotlinx.coroutines.CancellationException) { throw _
                } catch (e: Exception) { fail++; Log.record(TAG, "异常: ${e.message}") }
            }
        }

        // 方案B: 尝试 content.interact 视频红包（签到+分享可直接完成）
        if (success < maxCount) {
            try {
                val interactResult = BrowseVideoRpcCall.queryInteractTask()
                val json = try { JSONObject(interactResult) } catch (_: Exception) { null }
                if (json?.optBoolean("success") == true) {
                    // 签到
                    if (videoAutoSignIn.value) {
                        val signResult = BrowseVideoRpcCall.signIn()
                        if (JSONObject(signResult).optBoolean("success")) Log.other(TAG, "签到✅")
                    }
                    val tasks = json.optJSONArray("taskList")
                    if (tasks != null) {
                        Log.record(TAG, "方案B: 内容互动任务 ${tasks.length()} 个")
                        for (i in 0 until tasks.length()) {
                            if (success >= maxCount) break
                            val t = tasks.getJSONObject(i)
                            val taskType = t.optString("taskType", "")
                            val completed = t.optBoolean("completed", false)
                            if (completed) continue
                            // 只处理 signIn 和 wfDayShare（其他需要用户交互）
                            if (taskType == "signIn") {
                                val signR = BrowseVideoRpcCall.signIn()
                                if (JSONObject(signR).optBoolean("success")) { success++; Log.other(TAG, "签到完成✅") }
                            } else if (taskType == "wfDayShare") {
                                // 分享任务 - 尝试完成 IEP
                                val taskActivityId = t.optString("taskActivityId", "")
                                if (taskActivityId.isNotEmpty()) {
                                    val r = BrowseVideoRpcCall.finishIepTask(taskType, "ANTFARM", taskActivityId)
                                    if (JSONObject(r).optBoolean("success")) { success++; Log.other(TAG, "分享任务✅") }
                                    else fail++
                                }
                            }
                            delay(RandomUtil.nextLong(1000, 3000))
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        Log.record(TAG, "视频红包完成！成功: $success, 失败: $fail")
    }

    private fun extractContentId(url: String): String {
        return try {
            val idx = url.indexOf("&contentId=")
            if (idx < 0) return ""
            val start = idx + 11; val end = url.indexOf("&", start)
            if (end > start) url.substring(start, end) else url.substring(start)
        } catch (_: Exception) { "" }
    }
}
