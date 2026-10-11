package com.framework.model;

import java.lang.reflect.Method;

public class UrlMapping {
    private Class clazz;
    private Method method;

    public UrlMapping(Class className, Method methodName) {
        this.clazz = className;
        this.method = methodName;
    }

    public Class getClazz() {
        return clazz;
    }

    public Method getMethod() {
        return method;
    }

    @Override
    public String toString() {
        return clazz.getName() + "#" + method.getName();
    }
}