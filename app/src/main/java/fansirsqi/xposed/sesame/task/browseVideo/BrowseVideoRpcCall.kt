package fansirsqi.xposed.sesame.task.browseVideo

import fansirsqi.xposed.sesame.hook.RequestManager
import org.json.JSONArray
import org.json.JSONObject

/**
 * 浏览视频领红包 RPC 调用类
 * 用于自动完成支付宝「看视频领红包」每日任务
 */
object BrowseVideoRpcCall {

    private const val VERSION = "1.0"

    /**
     * 查询每日浏览任务列表
     * @return 响应字符串
     */
    fun queryTaskList(): String {
        return RequestManager.requestString(
            "com.alipay.adexchange.ad.facade.xlightPlugin",
            "[{\"action\":\"queryTaskList\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }

    /**
     * 完成任务上报 - 通用浏览任务
     * @param taskId 任务ID
     * @param taskType 任务类型
     * @return 响应字符串
     */
    fun reportBrowseComplete(taskId: String, taskType: String): String {
        return RequestManager.requestString(
            "com.alipay.adexchange.ad.facade.xlightPlugin",
            "[{\"action\":\"report\",\"taskId\":\"$taskId\",\"taskType\":\"$taskType\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }

    /**
     * 领取浏览奖励
     * @param taskId 任务ID
     * @return 响应字符串
     */
    fun receiveReward(taskId: String): String {
        return RequestManager.requestString(
            "com.alipay.adexchange.ad.facade.xlightPlugin",
            "[{\"action\":\"receiveReward\",\"taskId\":\"$taskId\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }

    /**
     * 查询红包奖励金额
     * @return 响应字符串
     */
    fun queryRedPacketAmount(): String {
        return RequestManager.requestString(
            "com.alipay.adexchange.ad.facade.xlightPlugin",
            "[{\"action\":\"queryAmount\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }

    /**
     * 查询内容浏览任务
     * @return 响应字符串
     */
    fun queryContentTasks(): String {
        return RequestManager.requestString(
            "alipay.content.browse.task.query",
            "[{\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }

    /**
     * 完成内容浏览任务
     * @param contentId 内容ID
     * @param duration 浏览时长(秒)
     * @return 响应字符串
     */
    fun finishContentTask(contentId: String, duration: Int): String {
        return RequestManager.requestString(
            "alipay.content.browse.task.finish",
            "[{\"contentId\":\"$contentId\",\"duration\":$duration,\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }

    /**
     * 领取内容浏览奖励
     * @param taskId 任务ID
     * @return 响应字符串
     */
    fun receiveContentReward(taskId: String): String {
        return RequestManager.requestString(
            "alipay.content.browse.task.receiveReward",
            "[{\"taskId\":\"$taskId\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
}
