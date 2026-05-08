package fansirsqi.xposed.sesame.task.browseVideo

import fansirsqi.xposed.sesame.hook.RequestManager
import org.json.JSONObject

/**
 * 浏览视频领红包 RPC 调用类
 * 使用支付宝真实的广告任务和内容浏览接口
 */
object BrowseVideoRpcCall {

    /**
     * 执行 AntFarm 日常任务（可用于视频/浏览类任务）
     * @param bizKey 任务业务Key，如 "WATCH_VIDEO", "BROWSE_CONTENT" 等
     */
    fun doFarmTask(bizKey: String, version: String): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.doFarmTask",
            "[{\"bizKey\":\"$bizKey\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\"$version\"}]"
        )
    }

    /**
     * 完成广告互动任务
     */
    fun finishAdTask(playBizId: String, playEventInfo: String, iepTaskType: String, iepTaskSceneCode: String): String {
        val extendInfo = JSONObject().apply {
            put("iepTaskSceneCode", iepTaskSceneCode)
            put("iepTaskType", iepTaskType)
            put("playEndingStatus", "success")
        }
        val args = JSONObject().apply {
            put("extendInfo", extendInfo)
            put("playBizId", playBizId)
            put("playEventInfo", playEventInfo)
            put("source", "adx")
        }
        return RequestManager.requestString(
            "com.alipay.adtask.biz.mobilegw.service.interaction.finish",
            "[$args]"
        )
    }

    /**
     * 完成普通任务
     */
    fun finishIepTask(taskType: String, sceneCode: String, outBizNo: String): String {
        val args = JSONObject().apply {
            put("outBizNo", outBizNo)
            put("requestType", "RPC")
            put("sceneCode", sceneCode)
            put("source", "ADBASICLIB")
            put("taskType", taskType)
        }
        return RequestManager.requestString(
            "com.alipay.antiep.finishTask",
            "[$args]"
        )
    }

    /**
     * 查询视频Tab URL（获取视频内容）
     */
    fun queryTabVideoUrl(version: String): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.queryTabVideoUrl",
            "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\"$version\"}]"
        )
    }

    /**
     * 内容阅读 - 下发内容模块
     */
    fun videoDeliverModule(bizId: String): String {
        return RequestManager.requestString(
            "alipay.content.reading.life.deliver.module",
            "[{\"bizId\":\"$bizId\",\"bizType\":\"CONTENT\",\"chInfo\":\"ch_antFarm\",\"refer\":\"antFarm\",\"timestamp\":\"${System.currentTimeMillis()}\"}]"
        )
    }

    /**
     * 内容阅读 - 触发奖励
     */
    fun videoTrigger(bizId: String): String {
        return RequestManager.requestString(
            "alipay.content.reading.life.prize.trigger",
            "[{\"bizId\":\"$bizId\",\"bizType\":\"CONTENT\",\"prizeFlowNum\":\"VIDEO_TASK\",\"prizeType\":\"farmFeed\"}]"
        )
    }
}
