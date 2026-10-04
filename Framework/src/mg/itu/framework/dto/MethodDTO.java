package mg.itu.framework.dto;

import java.lang.reflect.Method;

public class MethodDTO {
    private Class<?> clazz;
    private Method method;

    public Class<?> getClazz() {
        return clazz;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    @Override
    public String toString() {
        return clazz.getSimpleName() + " -> " + method.getName();
    }
}