package fansirsqi.xposed.sesame.task.browseVideo

import fansirsqi.xposed.sesame.hook.RequestManager

/**
 * 浏览视频领红包 RPC 调用
 * 使用 AntFarm 已验证的 videoDeliverModule + videoTrigger 流程
 */
object BrowseVideoRpcCall {

    /** 获取任务列表 */
    fun listFarmTask(version: String): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.listFarmTask",
            "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\"$version\"}]"
        )
    }

    /** 执行具体任务 */
    fun doFarmTask(bizKey: String, version: String): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.doFarmTask",
            "[{\"bizKey\":\"$bizKey\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\"$version\"}]"
        )
    }

    /** 下发视频内容（模拟请求视频资源） */
    fun videoDeliverModule(bizId: String): String {
        return RequestManager.requestString(
            "alipay.content.reading.life.deliver.module",
            "[{\"bizId\":\"$bizId\",\"bizType\":\"CONTENT\",\"chInfo\":\"ch_antFarm\",\"refer\":\"antFarm\",\"timestamp\":\"${System.currentTimeMillis()}\"}]"
        )
    }

    /** 触发视频观看奖励 */
    fun videoTrigger(bizId: String): String {
        return RequestManager.requestString(
            "alipay.content.reading.life.prize.trigger",
            "[{\"bizId\":\"$bizId\",\"bizType\":\"CONTENT\",\"prizeFlowNum\":\"VIDEO_TASK\",\"prizeType\":\"farmFeed\"}]"
        )
    }
}
