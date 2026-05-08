package fansirsqi.xposed.sesame.ui.dto;
import lombok.Data;
import java.io.Serializable;
import java.util.List;
/**
 * 模型数据传输对象。
 * 用于封装模型的代码、名称、组代码以及模型字段展示信息。
 */
@Data
public class ModelDto implements Serializable {
    /**
     * 模型代码。
     */
    private String modelCode;
    /**
     * 模型名称。
     */
    private String modelName;
    /**
     * 模型图标
     */
    private String modelIcon;
    /**
     * 组代码。
     */
    private String groupCode;
    /**
     * 模型字段展示信息列表。
     */
    private List<ModelFieldShowDto> modelFields;
    /**
     * 无参构造函数。
     */
    public ModelDto() {
    }
    /**
     * 全参构造函数。
     * @param modelCode 模型代码
     * @param modelName 模型名称
     * @param groupCode 组代码
     * @param modelFields 模型字段展示信息列表
     */
    public ModelDto(String modelCode, String modelName, String icon, String groupCode, List<ModelFieldShowDto> modelFields) {
        this.modelCode = modelCode;
        this.modelName = modelName;
        this.modelIcon = icon;
        this.groupCode = groupCode;
        this.modelFields = modelFields;
    }
}