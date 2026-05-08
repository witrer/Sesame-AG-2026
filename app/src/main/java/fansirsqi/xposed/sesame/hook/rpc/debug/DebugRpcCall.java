package fansirsqi.xposed.sesame.hook.rpc.debug;
import fansirsqi.xposed.sesame.hook.RequestManager;
public class DebugRpcCall {
    private static final String version = "2.0";
    public static String queryBaseinfo() {
        return RequestManager.requestString("com.alipay.neverland.biz.rpc.queryBaseinfo",
                "[{\"branchId\":\"WUFU\",\"source\":\"fuqiTown\"}]");
    }
    /** 行走格子 */
    public static String walkGrid() {
        return RequestManager.requestString("com.alipay.neverland.biz.rpc.walkGrid",
                "[{\"drilling\":false,\"mapId\":\"MF1\",\"source\":\"fuqiTown\"}]");
    }
    /** 小游戏 */
    public static String miniGameFinish(String gameId, String gameKey) {
        return RequestManager.requestString("com.alipay.neverland.biz.rpc.miniGameFinish",
                "[{\"gameId\":\"" + gameId + "\",\"gameKey\":\"" + gameKey
                        + "\",\"mapId\":\"MF1\",\"score\":490,\"source\":\"fuqiTown\"}]");
    }
    public static String taskFinish(String bizId) {
        return RequestManager.requestString("com.alipay.adtask.biz.mobilegw.service.task.finish",
                "[{\"bizId\":\"" + bizId + "\"}]");
    }
    public static String queryAdFinished(String bizId, String scene) {
        return RequestManager.requestString("com.alipay.neverland.biz.rpc.queryAdFinished",
                "[{\"adBizNo\":\"" + bizId + "\",\"scene\":\"" + scene
                        + "\",\"source\":\"fuqiTown\"}]");
    }
    public static String queryWufuTaskHall() {
        return RequestManager.requestString("com.alipay.neverland.biz.rpc.queryWufuTaskHall",
                "[{\"source\":\"fuqiTown\"}]");
    }
    public static String fuQiTaskQuery() {
        return RequestManager.requestString("com.alipay.wufudragonprod.biz.wufu2024.fuQiTown.fuQiTask.query",
                "[{}]");
    }
    public static String fuQiTaskTrigger(String appletId, String stageCode) {
        return RequestManager.requestString("com.alipay.wufudragonprod.biz.wufu2024.fuQiTown.fuQiTask.trigger",
                "[{\"appletId\":\"" + appletId + "\",\"stageCode\":\"" + stageCode + "\"}]");
    }
    public static String queryEnvironmentCertDetailList(String alias, int pageNum, String targetUserID) {
        return RequestManager.requestString("alipay.antforest.forest.h5.queryEnvironmentCertDetailList",
                "[{\"alias\":\"" + alias + "\",\"certId\":\"\",\"pageNum\":" + pageNum
                        + ",\"shareId\":\"\",\"source\":\"chInfo_ch_appcenter__chsub_9patch\",\"targetUserID\":\""
                        + targetUserID + "\",\"version\":\"20230701\"}]");
    }
    public static String sendTree(String certificateId, String friendUserId) {
        return RequestManager.requestString("alipay.antforest.forest.h5.sendTree",
                "[{\"blessWords\":\"梭梭没有叶子，四季常青，从不掉发，祝你发量如梭。\",\"certificateId\":\"" + certificateId
                        + "\",\"friendUserId\":\"" + friendUserId
                        + "\",\"source\":\"chInfo_ch_appcenter__chsub_9patch\"}]");
    }
}