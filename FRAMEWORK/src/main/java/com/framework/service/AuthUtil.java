package com.framework.service;

import com.framework.annotation.Auth;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.lang.reflect.Method;

public class AuthUtil {
    // Attributs de session que l'application doit renseigner à la connexion
    public static final String USER_KEY = "user";
    public static final String ROLE_KEY = "role";

    public static final int AUTORISE = 0;
    public static final int NON_AUTHENTIFIE = 401;
    public static final int INTERDIT = 403;

    // L'annotation de la méthode prime sur celle de la classe
    public static Auth getAuth(Class<?> classe, Method method) {
        Auth auth = method.getAnnotation(Auth.class);
        return auth != null ? auth : classe.getAnnotation(Auth.class);
    }

    public static int verifier(HttpServletRequest request, Class<?> classe, Method method) {
        Auth auth = getAuth(classe, method);
        if (auth == null) {
            return AUTORISE;
        }

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(USER_KEY) == null) {
            return NON_AUTHENTIFIE;
        }

        if (auth.roles().length == 0) {
            return AUTORISE;
        }

        Object role = session.getAttribute(ROLE_KEY);
        for (String autorise : auth.roles()) {
            if (autorise.equals(role)) {
                return AUTORISE;
            }
        }
        return INTERDIT;
    }
}
