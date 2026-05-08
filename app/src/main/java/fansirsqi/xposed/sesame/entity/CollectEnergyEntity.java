package fansirsqi.xposed.sesame.entity;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;
/**
 * 表示一个能量收集实体，包含用户信息及操作相关的状态。
 */
@Getter
public class CollectEnergyEntity {
    // 用户 ID
    public  final String userId;
    // 用户主页 JSON 对象
    @Setter
    public JSONObject userHome;
    // RPC 请求实体
    @Setter
    public RpcEntity rpcEntity;
    // 收集次数
    private Integer collectCount = 0;
    // 尝试次数
    private Integer tryCount = 0;
    // 是否需要翻倍
    @Setter
    public Boolean needDouble = false;
    // 是否需要重试
    @Setter
    public Boolean needRetry = false;
    // 收取来源标识
    @Setter
    public String fromTag;
    // 是否跳过道具检查（用于蹲点收取快速通道）
    @Setter
    public Boolean skipPropCheck = false;
    /**
     * 构造方法，仅指定用户 ID。
     * @param userId 用户 ID
     */
    public CollectEnergyEntity(String userId) {
        this.userId = userId;
    }
    /**
     * 构造方法，指定用户 ID 和用户主页信息。
     * @param userId 用户 ID
     * @param userHome 用户主页 JSON 对象
     */
    public CollectEnergyEntity(String userId, JSONObject userHome) {
        this.userId = userId;
        this.userHome = userHome;
    }
    /**
     * 构造方法，指定用户 ID、用户主页信息及 RPC 请求实体。
     * @param userId 用户 ID
     * @param userHome 用户主页 JSON 对象
     * @param rpcEntity RPC 请求实体
     */
    public CollectEnergyEntity(String userId, JSONObject userHome, RpcEntity rpcEntity) {
        this.userId = userId;
        this.userHome = userHome;
        this.rpcEntity = rpcEntity;
    }
    
    /**
     * 构造方法，指定用户 ID、用户主页信息、RPC 请求实体及来源标识。
     * @param userId 用户 ID
     * @param userHome 用户主页 JSON 对象
     * @param rpcEntity RPC 请求实体
     * @param fromTag 收取来源标识
     */
    public CollectEnergyEntity(String userId, JSONObject userHome, RpcEntity rpcEntity, String fromTag) {
        this.userId = userId;
        this.userHome = userHome;
        this.rpcEntity = rpcEntity;
        this.fromTag = fromTag;
    }
    
    /**
     * 构造方法，指定用户 ID、用户主页信息、RPC 请求实体、来源标识及是否跳过道具检查。
     * @param userId 用户 ID
     * @param userHome 用户主页 JSON 对象
     * @param rpcEntity RPC 请求实体
     * @param fromTag 收取来源标识
     * @param skipPropCheck 是否跳过道具检查（用于快速收取通道）
     */
    public CollectEnergyEntity(String userId, JSONObject userHome, RpcEntity rpcEntity, String fromTag, Boolean skipPropCheck) {
        this.userId = userId;
        this.userHome = userHome;
        this.rpcEntity = rpcEntity;
        this.fromTag = fromTag;
        this.skipPropCheck = skipPropCheck;
    }
    /**
     * 增加尝试次数。
     * @return 更新后的尝试次数
     */
    public Integer addTryCount() {
        this.tryCount += 1;
        return tryCount;
    }
    /**
     * 重置尝试次数为 0。
     */
    public void resetTryCount() {
        this.tryCount = 0;
    }
    /**
     * 设置需要翻倍，并增加收集次数。
     */
    public void setNeedDouble() {
        this.collectCount += 1;
        this.needDouble = true;
    }
    /**
     * 取消需要翻倍状态。
     */
    public void unsetNeedDouble() {
        this.needDouble = false;
    }
    /**
     * 设置需要重试状态。
     */
    public void setNeedRetry() {
        this.needRetry = true;
    }
    /**
     * 取消需要重试状态。
     */
    public void unsetNeedRetry() {
        this.needRetry = false;
    }
}
