package fansirsqi.xposed.sesame.ui.dto;
import lombok.Data;
import java.io.Serializable;
/**
 * 模型组数据传输对象。
 * 用于封装模型组的相关信息，包括组代码、名称和图标。
 */
@Data
public class ModelGroupDto implements Serializable {
    /**
     * 模型组代码。
     */
    private String code;
    /**
     * 模型组名称。
     */
    private String name;
    /**
     * 模型组图标。
     */
    private String icon;
    /**
     * 无参构造函数。
     * 用于反序列化等场景。
     */
    public ModelGroupDto() {
    }
    /**
     * 全参构造函数。
     * 用于创建包含完整信息的模型组对象。
     *
     * @param code 模型组代码
     * @param name 模型组名称
     * @param icon 模型组图标
     */
    public ModelGroupDto(String code, String name, String icon) {
        this.code = code;
        this.name = name;
        this.icon = icon;
    }
}