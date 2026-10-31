package com.framework.annotation;

import java.lang.annotation.*;

/**
 * Sur un paramètre de méthode : l'objet est construit à partir des champs du formulaire
 * préfixés par le nom du paramètre, avec la notation pointée pour les objets imbriqués.
 * Exemple : paramètre "client" -> client.nom, client.adresse.ville
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ObjectArg {
}
