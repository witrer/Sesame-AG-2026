package fansirsqi.xposed.sesame.ui;
import lombok.Data;
/**
 * 非线程安全的引用包装器。
 * 提供了一个泛型对象的非线程安全访问和修改。
 * @param <T> 泛型类型参数。
 */
@Data
public class ObjReference<T> {
    /**
     * 被包装的对象。
     */
    private T obj;
    /**
     * 无参构造函数。
     */
    public ObjReference() {
    }
    /**
     * 带对象的构造函数。
     * @param obj 被包装的对象。
     */
    public ObjReference(T obj) {
        this.obj = obj;
    }
    /**
     * 检查对象是否存在。
     * @return 如果对象不为空，返回true。
     */
    public Boolean has() {
        return this.obj != null;
    }
    /**
     * 获取被包装的对象。
     * @return 被包装的对象。
     */
    public T get() {
        return obj;
    }
    /**
     * 设置被包装的对象。
     * 如果当前对象与传入对象相同，或当前对象为null且传入对象不为null，则设置对象并返回true。
     * @param obj 新的对象。
     * @return 如果设置成功，返回true。
     */
    public Boolean set(T obj) {
        if (this.obj == obj) {
            return true;
        }
        if (this.obj != null) {
            return false;
        }
        this.obj = obj;
        return true;
    }
    /**
     * 强制设置被包装的对象。
     * 无论当前对象是什么，都会设置为传入的对象。
     * @param obj 新的对象。
     */
    public void setForce(T obj) {
        this.obj = obj;
    }
    /**
     * 删除被包装的对象。
     */
    public void del() {
        this.obj = null;
    }
    /**
     * 如果被包装的对象与传入对象相同，则删除。
     * @param obj 要比较的对象。
     */
    public void delIfEquals(T obj) {
        if (this.obj == obj) {
            this.obj = null;
        }
    }
}