package fansirsqi.xposed.sesame.ui.dto;
import org.json.JSONException;
import lombok.Data;
import fansirsqi.xposed.sesame.model.ModelField;
import java.io.Serializable;
/**
 * 模型字段信息数据传输对象。
 * 用于封装模型字段的详细信息，包括字段代码、名称、类型、扩展键、扩展值和配置值。
 */
@Data
public class ModelFieldInfoDto implements Serializable {
    /**
     * 字段代码。
     */
    private String code;
    /**
     * 字段名称。
     */
    private String name;
    /**
     * 字段类型。
     */
    private String type;
    /**
     * 扩展键，用于存储额外的信息。
     */
    private Object expandKey;
    /**
     * 扩展值，用于存储额外的信息。
     */
    private Object expandValue;
    /**
     * 配置值，用于存储字段的配置信息。
     */
    private String configValue;
    /**
     * 字段描述。
     */
    private String desc;
    /**
     * 无参构造函数。
     */
    public ModelFieldInfoDto() {
    }
    /**
     * 将ModelField对象转换为ModelFieldInfoDto对象。
     * @param modelField ModelField对象
     * @return ModelFieldInfoDto对象
     */
    public static ModelFieldInfoDto toInfoDto(ModelField<?> modelField) throws JSONException {
        ModelFieldInfoDto dto = new ModelFieldInfoDto();
        dto.setCode(modelField.getCode());
        dto.setName(modelField.getName());
        dto.setType(modelField.getType());
        dto.setExpandKey(modelField.getExpandKey());
        dto.setExpandValue(modelField.getExpandValue());
        dto.setConfigValue(modelField.getConfigValue());
        dto.setDesc(modelField.getDesc());
        return dto;
    }
}