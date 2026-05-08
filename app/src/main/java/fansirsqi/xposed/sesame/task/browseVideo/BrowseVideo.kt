package fansirsqi.xposed.sesame.task.browseVideo

import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.model.ModelGroup
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.RandomUtil
import kotlinx.coroutines.delay
import org.json.JSONObject

/**
 * 首页视频红包 — 看视频领现金
 * 通过 alipay.content.interact.task.query 获取任务，promoprod.play.trigger 触发完成
 */
class BrowseVideo : ModelTask() {

    companion object {
        private const val TAG = "BrowseVideo"
        const val MODULE_NAME = "视频红包"

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
        addField(IntegerModelField("videoMaxCount", "视频红包 | 每日最大次数", 5, 1, 50).also { videoMaxCount = it })
        addField(IntegerModelField("videoBrowseDuration", "视频红包 | 模拟浏览时长(秒)", 30, 10, 60).also { videoBrowseDuration = it })
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

        // 1. 查询互动任务列表
        val result = BrowseVideoRpcCall.queryInteractTask()
        val json = try { JSONObject(result) } catch (_: Exception) { null }
        if (json == null || !json.optBoolean("success")) {
            Log.record(TAG, "查询视频任务失败")
            return
        }

        // 2. 顺便签到
        if (videoAutoSignIn.value) {
            try {
                val signResult = BrowseVideoRpcCall.signIn()
                val signJson = JSONObject(signResult)
                if (signJson.optBoolean("success")) {
                    Log.other(TAG, "签到成功✅")
                }
            } catch (_: Exception) {}
        }

        val taskList = json.optJSONArray("taskList") ?: return
        Log.record(TAG, "共 ${taskList.length()} 个任务")

        val traceId = json.optString("traceId", "")
        val extInfo = json.optJSONObject("extInfo")
        val captchaId = extInfo?.optString("captchaId", "") ?: ""

        for (i in 0 until taskList.length()) {
            if (success >= maxCount) break
            try {
                val task = taskList.getJSONObject(i)
                val taskType = task.optString("taskType", "")

                // 只处理 video/radicalRed 类任务
                if (taskType != "radicalRed" && taskType != "videoTask") continue
                if (task.optBoolean("completed")) continue

                delay(RandomUtil.nextLong(2000, 5000))

                val taskActivityId = task.optString("taskActivityId", "")
                val uniqTaskId = task.optString("uniqTaskId", "")
                val origTaskType = task.optString("origTaskType", taskType)
                val rewardAmount = task.optJSONObject("taskData")?.optString("availableAmount", "0") ?: "0"
                val taskDuration = task.optJSONObject("taskData")?.optInt("duration", duration) ?: duration

                Log.record(TAG, "[${success + 1}/$maxCount] 视频任务: $origTaskType (${taskDuration}秒, ¥$rewardAmount)")

                // 3. 模拟观看
                val waitMs = ((taskDuration + RandomUtil.nextInt(-3, 5)) * 1000L).coerceAtLeast(5000)
                delay(waitMs)

                // 4. 触发完成
                val triggerResult = BrowseVideoRpcCall.triggerPromoPlay(
                    30, origTaskType, taskActivityId, uniqTaskId
                )
                val triggerJson = try { JSONObject(triggerResult) } catch (_: Exception) { null }

                if (triggerJson?.optBoolean("success") == true) {
                    success++
                    Log.other(TAG, "视频红包完成🧧[$origTaskType] +¥$rewardAmount")
                } else {
                    fail++
                    Log.record(TAG, "视频任务失败[$origTaskType]: ${triggerResult.take(100)}")
                }

            } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (e: Exception) { fail++; Log.record(TAG, "异常: ${e.message}") }
        }

        Log.record(TAG, "视频红包完成！成功: $success, 失败: $fail")
    }
}
