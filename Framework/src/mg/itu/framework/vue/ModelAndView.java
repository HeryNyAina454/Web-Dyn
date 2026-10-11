package mg.itu.framework.vue;

import java.util.HashMap;

public class ModelAndView {
    private String view;
    private HashMap<String, Object> data;

    public ModelAndView() {
    }

    public ModelAndView(String view) {
        this.view = view;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public HashMap<String, Object> getData() {
        return data;
    }

    public void addAttribute(String key, Object value) {
        if (this.data == null)
            this.data = new HashMap<>();
        this.data.put(key, value);
    }
}