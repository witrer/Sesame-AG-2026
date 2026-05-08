package fansirsqi.xposed.sesame.ui.dto;
import lombok.Data;
import fansirsqi.xposed.sesame.model.ModelField;
import java.io.Serializable;
/**
 * 模型字段展示数据传输对象。
 * 用于封装模型字段的展示信息，包括字段代码、名称、类型、扩展键和配置值。
 */
@Data
public class ModelFieldShowDto implements Serializable {
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
    public ModelFieldShowDto() {
    }
    /**
     * 将ModelField对象转换为ModelFieldShowDto对象。
     * 这是一个静态工厂方法，用于创建ModelFieldShowDto实例。
     *
     * @param modelField ModelField对象
     * @return ModelFieldShowDto对象
     */
    public static ModelFieldShowDto toShowDto(ModelField<?> modelField) {
        ModelFieldShowDto dto = new ModelFieldShowDto();
        dto.setCode(modelField.getCode());
        dto.setName(modelField.getName());
        dto.setType(modelField.getType());
        dto.setExpandKey(modelField.getExpandKey());
        dto.setConfigValue(modelField.getConfigValue());
        dto.setDesc(modelField.getDesc());
        return dto;
    }
}