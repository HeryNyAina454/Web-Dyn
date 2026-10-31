package com.framework.annotation;

import java.lang.annotation.*;

/**
 * Protège une méthode (ou toute une classe contrôleur).
 * Sans rôle : il suffit d'être connecté (attribut de session "user").
 * Avec rôles : l'attribut de session "role" doit être l'un d'eux.
 * L'annotation de la méthode prime sur celle de la classe.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Auth {
    String[] roles() default {};
}
