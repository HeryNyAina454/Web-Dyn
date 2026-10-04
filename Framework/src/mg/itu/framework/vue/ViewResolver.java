package mg.itu.framework.vue;

public class ViewResolver {
    private String prefix;
    private String suffix;

    public ViewResolver(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public String resolve(String viewName) {
        return prefix + viewName + suffix;
    }

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }

    public String getSuffix() { return suffix; }
    public void setSuffix(String suffix) { this.suffix = suffix; }

    // getters/setters
}