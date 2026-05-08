package fansirsqi.xposed.sesame.util;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
/**
 * 类型工具类。
 * 提供了一系列方法来处理Java反射中的类型相关的操作。
 */
public class TypeUtil {
    /**
     * 私有构造函数，防止实例化。
     */
    private TypeUtil() {
    }
    /**
     * 从给定的类型中提取Class对象。
     * 如果类型是Class、ParameterizedType或有界的TypeVariable/WildcardType，则返回其对应的Class对象。
     *
     * @param type 给定的类型。
     * @return 提取的Class对象，如果无法提取则返回null。
     */
    public static Class<?> getClass(Type type) {
        if (type != null) {
            if (type instanceof Class<?>) {
                return (Class<?>) type;
            }
            if (type instanceof ParameterizedType) {
                return (Class<?>) ((ParameterizedType) type).getRawType();
            }
            Type[] upperBounds;
            if (type instanceof TypeVariable<?>) {
                upperBounds = ((TypeVariable<?>) type).getBounds();
                if (upperBounds.length == 1) {
                    return getClass(upperBounds[0]);
                }
            } else if (type instanceof WildcardType) {
                upperBounds = ((WildcardType) type).getUpperBounds();
                if (upperBounds.length == 1) {
                    return getClass(upperBounds[0]);
                }
            }
        }
        return null;
    }
    /**
     * 获取Field的泛型类型。
     *
     * @param field Field对象。
     * @return Field的泛型类型。
     */
    public static Type getType(Field field) {
        return field != null ? field.getGenericType() : null;
    }
    /**
     * 获取Field的Class类型。
     *
     * @param field Field对象。
     * @return Field的Class类型。
     */
    public static Class<?> getClass(Field field) {
        return field != null ? field.getType() : null;
    }
    /**
     * 获取Method的第一个参数的泛型类型。
     *
     * @param method Method对象。
     * @return 第一个参数的泛型类型。
     */
    public static Type getFirstParamType(Method method) {
        return getParamType(method, 0);
    }
    /**
     * 获取Method的第一个参数的Class类型。
     *
     * @param method Method对象。
     * @return 第一个参数的Class类型。
     */
    public static Class<?> getFirstParamClass(Method method) {
        return getParamClass(method, 0);
    }
    /**
     * 获取Method指定索引位置参数的泛型类型。
     *
     * @param method Method对象。
     * @param index  参数索引。
     * @return 指定索引位置参数的泛型类型。
     */
    public static Type getParamType(Method method, int index) {
        Type[] types = getParamTypes(method);
        return types != null && types.length > index ? types[index] : null;
    }
    /**
     * 获取Method指定索引位置参数的Class类型。
     *
     * @param method Method对象。
     * @param index  参数索引。
     * @return 指定索引位置参数的Class类型。
     */
    public static Class<?> getParamClass(Method method, int index) {
        Class<?>[] classes = getParamClasses(method);
        return classes != null && classes.length > index ? classes[index] : null;
    }
    /**
     * 获取Method的所有参数的泛型类型。
     *
     * @param method Method对象。
     * @return Method的所有参数的泛型类型。
     */
    public static Type[] getParamTypes(Method method) {
        return method != null ? method.getGenericParameterTypes() : null;
    }
    /**
     * 获取Method的所有参数的Class类型。
     *
     * @param method Method对象。
     * @return Method的所有参数的Class类型。
     */
    public static Class<?>[] getParamClasses(Method method) {
        return method != null ? method.getParameterTypes() : null;
    }
    /**
     * 获取Method的返回值的泛型类型。
     *
     * @param method Method对象。
     * @return Method的返回值的泛型类型。
     */
    public static Type getReturnType(Method method) {
        return method != null ? method.getGenericReturnType() : null;
    }
    /**
     * 获取Method的返回值的Class类型。
     *
     * @param method Method对象。
     * @return Method的返回值的Class类型。
     */
    public static Class<?> getReturnClass(Method method) {
        return method != null ? method.getReturnType() : null;
    }
    /**
     * 获取泛型类型的参数类型。
     *
     * @param type 泛型类型。
     * @return 参数类型。
     */
    public static Type getTypeArgument(Type type) {
        return getTypeArgument(type, 0);
    }
    /**
     * 获取泛型类型的指定索引位置的参数类型。
     *
     * @param type  泛型类型。
     * @param index 参数索引。
     * @return 指定索引位置的参数类型。
     */
    public static Type getTypeArgument(Type type, int index) {
        Type[] typeArguments = getTypeArguments(type);
        return typeArguments != null && typeArguments.length > index ? typeArguments[index] : null;
    }
    /**
     * 获取泛型类型的所有参数类型。
     *
     * @param type 泛型类型。
     * @return 泛型类型的所有参数类型。
     */
    public static Type[] getTypeArguments(Type type) {
        if (type == null) {
            return null;
        }
        ParameterizedType parameterizedType = toParameterizedType(type);
        return parameterizedType != null ? parameterizedType.getActualTypeArguments() : null;
    }
    /**
     * 将类型转换为ParameterizedType。
     *
     * @param type 泛型类型。
     * @return ParameterizedType对象。
     */
    public static ParameterizedType toParameterizedType(Type type) {
        return toParameterizedType(type, 0);
    }
    /**
     * 将类型转换为ParameterizedType，并指定接口索引。
     *
     * @param type           泛型类型。
     * @param interfaceIndex 接口索引。
     * @return ParameterizedType对象。
     */
    public static ParameterizedType toParameterizedType(Type type, int interfaceIndex) {
        if (type instanceof ParameterizedType) {
            return (ParameterizedType) type;
        } else if (type instanceof Class<?>) {
            ParameterizedType[] generics = getGenerics((Class<?>) type);
            if (generics.length > interfaceIndex) {
                return generics[interfaceIndex];
            }
        }
        return null;
    }
    /**
     * 获取类的泛型类型。
     *
     * @param clazz Class对象。
     * @return 泛型类型数组。
     */
    public static ParameterizedType[] getGenerics(Class<?> clazz) {
        List<ParameterizedType> result = new ArrayList<>();
        Type genericSuper = clazz.getGenericSuperclass();
        if (genericSuper != null && !Object.class.equals(genericSuper)) {
            ParameterizedType parameterizedType = toParameterizedType(genericSuper);
            if (parameterizedType != null) {
                result.add(parameterizedType);
            }
        }
        Type[] genericInterfaces = clazz.getGenericInterfaces();
        for (Type genericInterface : genericInterfaces) {
            ParameterizedType parameterizedType = toParameterizedType(genericInterface);
            if (parameterizedType != null) {
                result.add(parameterizedType);
            }
        }
        return result.toArray(new ParameterizedType[0]);
    }
    /**
     * 检查类型是否未知。
     *
     * @param type 给定的类型。
     * @return 如果类型未知或为TypeVariable，则返回true。
     */
    public static boolean isUnknown(Type type) {
        return type == null || type instanceof TypeVariable;
    }
    /**
     * 检查给定的类型数组中是否包含TypeVariable。
     *
     * @param types 类型数组。
     * @return 如果包含TypeVariable，则返回true。
     */
    public static boolean hasTypeVariable(Type... types) {
        for (Type type : types) {
            if (type instanceof TypeVariable) {
                return true;
            }
        }
        return false;
    }
}