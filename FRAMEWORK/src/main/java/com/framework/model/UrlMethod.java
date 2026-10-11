package com.framework.model;

import java.util.Objects;

public class UrlMethod {

    private String url;
    private String method;

    public UrlMethod(String url, String method) {
        this.url = url;
        this.method = method.toUpperCase();
    }

    public String getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UrlMethod)) return false;

        UrlMethod other = (UrlMethod) o;

        return url.equals(other.url)
                && method.equals(other.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, method);
    }

    @Override
    public String toString() {
        return "UrlMethod{" +
                "url='" + url + '\'' +
                ", method='" + method + '\'' +
                '}';
    }
}
