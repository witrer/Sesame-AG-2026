package fansirsqi.xposed.sesame.task.browseVideo

import fansirsqi.xposed.sesame.hook.RequestManager
import org.json.JSONObject

/**
 * 首页视频红包 RPC 调用
 * 通过 alipay.content.interact.task.query 获取任务
 */
object BrowseVideoRpcCall {

    /** 查询内容互动任务（首页视频红包入口） */
    fun queryInteractTask(pageType: String = "index", tabType: String = "discovery.featured"): String {
        val ext = JSONObject().apply {
            put("fromTab3BottomBar", true)
            put("openTab3", true)
            put("retryCount", 0)
            put("tabType", tabType)
        }
        val args = JSONObject().apply {
            put("action", "launch")
            put("carrying", 0)
            put("pageType", pageType)
            put("tab3SpecialVer", "normal")
            put("taskExt", ext.toString())
        }
        return RequestManager.requestString(
            "alipay.content.interact.task.query",
            "[$args]"
        )
    }

    /** 完成广告/视频任务 */
    fun finishAdTask(playBizId: String, playEventInfo: String, taskType: String, sceneCode: String): String {
        val extendInfo = JSONObject().apply {
            put("iepTaskSceneCode", sceneCode)
            put("iepTaskType", taskType)
            put("playEndingStatus", "success")
        }
        val args = JSONObject().apply {
            put("extendInfo", extendInfo)
            put("playBizId", playBizId)
            put("playEventInfo", playEventInfo)
            put("source", "adx")
        }
        return RequestManager.requestString(
            "com.alipay.adtask.biz.mobilegw.service.task.finish",
            "[$args]"
        )
    }

    /** 触发营销活动完成 */
    fun triggerPromoPlay(sceneCode: String, taskType: String, taskActivityId: String, uniqTaskId: String): String {
        val args = JSONObject().apply {
            put("osc", 0)
            put("ot", taskType)
            put("s", 20)
            put("sc", sceneCode)
            put("t", 15600001)
            put("tsq", 0)
            put("tt", "radicalRed")
            put("ts", System.currentTimeMillis() / 1000)
            put("trf3", false)
            put("cp", taskActivityId)
            put("lti", uniqTaskId)
        }
        return RequestManager.requestString(
            "com.alipay.promoprod.play.trigger",
            "[$args]"
        )
    }

    /** 领取任务奖励 - 通过 IEP 任务完成 */
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

    /** AntFarm 任务列表 */
    fun listFarmTask(version: String): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.listFarmTask",
            "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\"$version\"}]"
        )
    }

    /** AntFarm 执行任务 */
    fun doFarmTask(bizKey: String, version: String): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.doFarmTask",
            "[{\"bizKey\":\"$bizKey\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\"$version\"}]"
        )
    }

    /** 下发视频内容 */
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

    /** 签到领取 */
    fun signIn(sceneCode: String = "ANTFARM"): String {
        return RequestManager.requestString(
            "com.alipay.mrchservbase.mrchpoint.sqyj.homepage.signin.v1",
            "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"$sceneCode\",\"source\":\"H5\"}]"
        )
    }
}
