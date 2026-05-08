package fansirsqi.xposed.sesame.task.browseVideo

import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.model.ModelGroup
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.StringModelField
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.RandomUtil
import kotlinx.coroutines.delay
import org.json.JSONObject

/**
 * 浏览视频领红包任务模块
 * 使用支付宝 adtask/antiep 真实接口完成每日视频浏览任务
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
    private lateinit var videoAutoClaim: BooleanModelField
    private lateinit var videoTaskBizKey: StringModelField

    override fun getName(): String = MODULE_NAME
    override fun getGroup(): ModelGroup = ModelGroup.OTHER
    override fun getIcon(): String = "Default.png"

    override fun getFields(): ModelFields {
        return ModelFields().apply {
            addField(BooleanModelField("videoAutoBrowse", "视频红包 | 自动浏览", true).also {
                videoAutoBrowse = it
            })
            addField(IntegerModelField("videoMaxCount", "视频红包 | 每日最大次数", 10, 1, 50).also {
                videoMaxCount = it
            })
            addField(IntegerModelField("videoBrowseDuration", "视频红包 | 每次浏览时长(秒)", 15, 5, 60).also {
                videoBrowseDuration = it
            })
            addField(BooleanModelField("videoAutoClaim", "视频红包 | 自动领取奖励", true).also {
                videoAutoClaim = it
            })
            addField(StringModelField("videoTaskBizKey", "视频红包 | 任务BizKey(逗号分隔)", "WATCH_VIDEO").also {
                videoTaskBizKey = it
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
        val bizKeys = videoTaskBizKey.value.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        Log.record(TAG, "开始浏览视频任务，目标次数: $maxCount, BizKeys: $bizKeys")

        var successCount = 0
        var failCount = 0

        for (i in 1..maxCount) {
            try {
                delay(RandomUtil.nextLong(3000, 6000))

                // 遍历所有 bizKey 尝试完成任务
                var taskCompleted = false
                for (bizKey in bizKeys) {
                    // 1. 执行任务
                    val taskResult = BrowseVideoRpcCall.doFarmTask(bizKey, VERSION)
                    if (taskResult.isNotEmpty()) {
                        val json = try { JSONObject(taskResult) } catch (_: Exception) { null }
                        val success = json?.optBoolean("success", false) ?: false

                        if (success) {
                            // 2. 模拟浏览时间
                            val duration = browseDuration + RandomUtil.nextInt(-3, 3)
                            Log.record(TAG, "[$i/$maxCount] $bizKey 任务提交成功，模拟浏览 ${duration}秒")
                            delay(duration * 1000L)

                            // 3. 尝试完成 IEP 任务
                            if (videoAutoClaim.value) {
                                delay(RandomUtil.nextLong(2000, 4000))
                                BrowseVideoRpcCall.finishIepTask(bizKey, "ANTFARM", "browse_${System.currentTimeMillis()}")
                            }

                            taskCompleted = true
                            break
                        }
                    }
                }

                if (taskCompleted) {
                    successCount++
                    Log.record(TAG, "[$i/$maxCount] 视频任务完成")
                } else {
                    failCount++
                    Log.record(TAG, "[$i/$maxCount] 无可用任务")
                }

            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.record(TAG, "视频浏览任务被中断")
                throw e
            } catch (e: Exception) {
                failCount++
                Log.record(TAG, "[$i/$maxCount] 视频任务异常: ${e.message}")
            }
        }

        Log.record(TAG, "视频红包任务完成！成功: $successCount, 失败: $failCount")
    }
}
