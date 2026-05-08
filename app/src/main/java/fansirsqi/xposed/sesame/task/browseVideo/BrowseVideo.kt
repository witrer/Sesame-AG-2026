package fansirsqi.xposed.sesame.task.browseVideo

import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.model.ModelGroup
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.util.JsonUtil
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.RandomUtil
import org.json.JSONObject

/**
 * 浏览视频领红包任务模块
 * 自动完成支付宝「看视频领红包」每日任务
 */
class BrowseVideo : ModelTask() {

    companion object {
        private const val TAG = "BrowseVideo"
        const val MODULE_NAME = "视频红包"

        @Volatile
        var instance: BrowseVideo? = null
            private set
    }

    // 配置字段
    private lateinit var videoAutoBrowse: BooleanModelField
    private lateinit var videoMaxCount: IntegerModelField
    private lateinit var videoBrowseDuration: IntegerModelField
    private lateinit var videoAutoClaim: BooleanModelField
    private lateinit var videoContentTask: BooleanModelField

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
            addField(BooleanModelField("videoContentTask", "视频红包 | 内容浏览任务", true).also {
                videoContentTask = it
            })
        }
    }

    override fun prepare() {
        instance = this
        Log.record(TAG, "视频红包模块准备完成")
    }

    override fun boot(clazz: ClassLoader?) {
        super.boot(clazz)
        Log.record(TAG, "视频红包模块启动完成")
    }

    override fun destroy() {
        instance = null
        super.destroy()
    }

    override fun run() {
        if (!videoAutoBrowse.value) {
            Log.record(TAG, "视频红包自动浏览未开启")
            return
        }

        var successCount = 0
        var failCount = 0
        val maxCount = videoMaxCount.value
        val browseDuration = videoBrowseDuration.value

        Log.record(TAG, "开始浏览视频任务，目标次数: $maxCount，每次时长: ${browseDuration}秒")

        for (i in 1..maxCount) {
            try {
                Thread.sleep(RandomUtil.nextLong(2000, 5000))

                // 1. 查询任务
                val taskResult = queryAndGetTask()
                if (taskResult == null) {
                    failCount++
                    Log.record(TAG, "[$i/$maxCount] 未找到可用任务，跳过")
                    continue
                }

                val taskId = taskResult.optString("taskId", "")
                val contentId = taskResult.optString("contentId", "")

                if (taskId.isEmpty() && contentId.isEmpty()) {
                    failCount++
                    continue
                }

                // 2. 模拟浏览
                val duration = browseDuration + RandomUtil.nextInt(-3, 3)
                Log.record(TAG, "[$i/$maxCount] 模拟浏览: ${duration}秒")
                Thread.sleep(duration * 1000L)

                // 3. 完成任务
                if (contentId.isNotEmpty()) {
                    BrowseVideoRpcCall.finishContentTask(contentId, duration)
                } else {
                    BrowseVideoRpcCall.reportBrowseComplete(taskId, "browse")
                }

                // 4. 领取奖励
                if (videoAutoClaim.value) {
                    Thread.sleep(RandomUtil.nextLong(1000, 3000))
                    val rewardResult = BrowseVideoRpcCall.receiveReward(taskId)
                    if (rewardResult.isNotEmpty()) {
                        val rewardJson = try { JSONObject(rewardResult) } catch (_: Exception) { null }
                        val amount = rewardJson?.optString("amount", "") ?: ""
                        if (amount.isNotEmpty()) {
                            Log.other(TAG, "视频红包奖励: $amount 元")
                        }
                    }
                }

                successCount++
                Log.record(TAG, "[$i/$maxCount] 视频任务完成")

            } catch (e: InterruptedException) {
                Log.record(TAG, "视频浏览任务被中断")
                break
            } catch (e: Exception) {
                failCount++
                Log.record(TAG, "[$i/$maxCount] 视频任务异常: ${e.message}")
            }
        }

        Log.record(TAG, "视频红包任务完成！成功: $successCount, 失败: $failCount")
    }

    /**
     * 查询并获取一个可执行的任务
     */
    private fun queryAndGetTask(): JSONObject? {
        return try {
            // 先尝试内容浏览任务
            if (videoContentTask.value) {
                val response = BrowseVideoRpcCall.queryContentTasks()
                if (response.isNotEmpty()) {
                    val json = try { JSONObject(response) } catch (_: Exception) { null }
                    val tasks = json?.optJSONArray("tasks")
                    if (tasks != null && tasks.length() > 0) {
                        return tasks.getJSONObject(0)
                    }
                }
            }

            // 备用：广告浏览任务
            val response = BrowseVideoRpcCall.queryTaskList()
            if (response.isNotEmpty()) {
                val json = try { JSONObject(response) } catch (_: Exception) { null }
                val tasks = json?.optJSONArray("taskList")
                if (tasks != null && tasks.length() > 0) {
                    return tasks.getJSONObject(0)
                }
            }
            null
        } catch (e: Exception) {
            Log.record(TAG, "查询任务失败: ${e.message}")
            null
        }
    }
}
