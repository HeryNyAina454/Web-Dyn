package com.framework.model;

import java.util.HashMap;

public class VueData {

    private String vue;
    private HashMap<String, Object> data = new HashMap<>();

    public VueData() {
    }

    public String getVue() {
        return vue;
    }

    public HashMap<String, Object> getData() {
        return data;
    }

    public void setVue(String vue) {
        this.vue = vue;
    }

    public void setData(HashMap<String, Object> data) {
        this.data = data;
    }

    public void setData(String key, Object value) {
        if (this.data == null) {
            this.data = new HashMap<>();
        }
        this.data.put(key, value);
    }
}
