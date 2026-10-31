package com.framework.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Copie de la session HTTP, propre à une requête. Le framework la remplit avant l'appel de la
 * méthode du contrôleur et recopie les changements dans la vraie session après l'appel.
 */
public class MapSession {
    private Map<String, Object> attributes = new HashMap<>();
    private boolean invalide = false;

    public void add(String key, Object value) {
        attributes.put(key, value);
    }

    public void setAttribute(String key, Object value) {
        add(key, value);
    }

    public Object get(String key) {
        return attributes.get(key);
    }

    public Object getAttribute(String key) {
        return get(key);
    }

    public void remove(String key) {
        attributes.remove(key);
    }

    public void removeAttribute(String key) {
        remove(key);
    }

    public void clearAttributes() {
        attributes.clear();
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Set<String> getKeys() {
        return attributes.keySet();
    }

    /** Demande la fermeture de la session (déconnexion) à la fin de la méthode. */
    public void invalidate() {
        this.invalide = true;
    }

    public boolean isInvalide() {
        return invalide;
    }
}
