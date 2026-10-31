package com.framework.service;

import com.framework.annotation.Controller;
import com.framework.annotation.FrontMapping;
import com.framework.annotation.ObjectArg;
import com.framework.annotation.Url;
import com.framework.model.MapFile;
import com.framework.model.UrlMapping;
import com.framework.model.UrlMethod;
import com.framework.model.VueData;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.net.JarURLConnection;

public class Utils {
    public static List<String> getControllers(String packageName) {
        List<Class<?>> classes = findClass(packageName);
        List<String> controllerNames = new ArrayList<>();
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(com.framework.annotation.Controller.class)) {
                controllerNames.add(clazz.getName());
            }
        }
        return controllerNames;
    }

    public static List<Class<?>> findClass(String packageName) {
        String packagePath = packageName.replace('.', '/');
        List<Class<?>> classes = new ArrayList<>();
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL packageURL = classLoader.getResource(packagePath);
            if (packageURL == null) {
                throw new RuntimeException("Package not found: " + packageName);
            }

            String protocol = packageURL.getProtocol();

            if ("file".equals(protocol)) {
                File directory = new File(packageURL.toURI());
                scanDirectory(directory, packageName, classes);

            } else if ("jar".equals(protocol)) {
                JarURLConnection jarConn = (JarURLConnection) packageURL.openConnection();
                JarFile jarFile = jarConn.getJarFile();
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    if (entryName.startsWith(packagePath + "/")
                            && entryName.endsWith(".class")
                            && !entry.isDirectory()) {
                        String className = entryName.replace('/', '.').replace(".class", "");
                        classes.add(Class.forName(className));
                    }
                }

            } else {
                throw new RuntimeException("Unsupported protocol '" + protocol + "' for package: " + packageName);
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error loading classes from package: " + packageName, e);
        }
        return classes;
    }

    private static void scanDirectory(File directory, String packageName, List<Class<?>> classes) throws Exception {
        if (!directory.exists() || !directory.isDirectory()) return;

        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                classes.add(Class.forName(className));
            }
        }
    }

    public static Map<String, UrlMapping> getMappings(String packageName) {
        List<Class<?>> classes = findClass(packageName);
        Map<String, UrlMapping> mappings = new HashMap<>();

        for (Class<?> clazz : classes) {
            if (!clazz.isAnnotationPresent(Controller.class)) continue;

            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(FrontMapping.class)) {
                    String url = method.getAnnotation(FrontMapping.class).value();
                    if (mappings.containsKey(url)) {
                        throw new RuntimeException("URL en double : '" + url + "' déjà mappée par " + mappings.get(url));
                    }
                    mappings.put(url, new UrlMapping(clazz, method));
                }
            }
        }
        return mappings;
    }

    public static Map<UrlMethod, UrlMapping> getMappingsAvecMethod(String packageName) {
        List<Class<?>> classes = findClass(packageName);
        Map<UrlMethod, UrlMapping> mappings = new HashMap<>();

        for (Class<?> clazz : classes) {
            if (!clazz.isAnnotationPresent(Controller.class)) continue;

            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Url.class)) {

                    Url annotation = method.getAnnotation(Url.class);

                    String url = annotation.value();
                    String httpMethod = annotation.method();

                    UrlMethod urlMethod = new UrlMethod(url, httpMethod);

                    if (mappings.containsKey(urlMethod)) {
                        throw new RuntimeException(
                                "URL en double : '" + url +
                                        "' déjà mappée par " + mappings.get(urlMethod)
                        );
                    }

                    mappings.put(urlMethod, new UrlMapping(clazz, method));
                }
            }
        }
        return mappings;
    }

    public static String formatterVue(String vue, ViewPath viewPath) {
        return viewPath.getPrefix() + vue + viewPath.getSuffix();
    }

    public static void redirigerRequete(Object result, ViewPath viewPath, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (result instanceof String) {
            String vue = formatterVue((String) result, viewPath);

            request.getRequestDispatcher(vue).forward(request, response);
        } else if (result instanceof VueData) {
            VueData vueData = (VueData) result;

            if (vueData.getData() != null) {
                for (Map.Entry<String, Object> entry : vueData.getData().entrySet()) {
                    request.setAttribute(entry.getKey(), entry.getValue());
                }
            }

            String vue = formatterVue(vueData.getVue(), viewPath);
            request.getRequestDispatcher(vue).forward(request, response);

        }
    }

    // =====================================================================
    //  Binding des arguments de la méthode du contrôleur
    // =====================================================================

    private static final int PROFONDEUR_MAX = 5;

    public static Object[] construireArguments(Method method, HttpServletRequest request) throws Exception {
        Parameter[] parametres = method.getParameters();
        Object[] arguments = new Object[parametres.length];

        for (int i = 0; i < parametres.length; i++) {
            Parameter p = parametres[i];
            Class<?> type = p.getType();

            if (type == MapFile.class) {
                arguments[i] = lireMapFile(request, p.getName());
            } else if (type == MapFile[].class) {
                arguments[i] = lireMapFiles(request, p.getName());
            } else if (type == Part.class) {
                arguments[i] = lirePart(request, p.getName());
            } else if (estTypeSimple(type)) {
                String valeur = request.getParameter(p.getName());
                arguments[i] = convertir(valeur, type);
            } else {
                // @ObjectArg : champs préfixés par le nom du paramètre (client.nom, client.adresse.ville)
                // sinon : champs sans préfixe (nom, adresse.ville)
                String prefixe = p.isAnnotationPresent(ObjectArg.class) ? p.getName() : "";
                arguments[i] = construireObjet(type, prefixe, request);
            }
        }
        return arguments;
    }

    private static boolean estTypeSimple(Class<?> type) {
        return type == String.class
                || type == int.class || type == Integer.class
                || type == long.class || type == Long.class
                || type == double.class || type == Double.class
                || type == float.class || type == Float.class
                || type == boolean.class || type == Boolean.class
                || type == short.class || type == Short.class
                || type == byte.class || type == Byte.class
                || type == char.class || type == Character.class
                || type == Date.class
                || type == LocalDate.class
                || type == LocalDateTime.class
                || type == UUID.class
                || type.isEnum();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object convertir(String valeur, Class<?> type) throws Exception {
        if (valeur == null || valeur.isBlank()) {
            if (type == int.class)
                return 0;
            if (type == long.class)
                return 0L;
            if (type == double.class)
                return 0.0;
            if (type == float.class)
                return 0f;
            if (type == boolean.class)
                return false;
            if (type == short.class)
                return (short) 0;
            if (type == byte.class)
                return (byte) 0;
            if (type == char.class)
                return '\0';
            return null;
        }
        if (type == String.class)
            return valeur;
        if (type == int.class || type == Integer.class)
            return Integer.parseInt(valeur);
        if (type == long.class || type == Long.class)
            return Long.parseLong(valeur);
        if (type == double.class || type == Double.class)
            return Double.parseDouble(valeur);
        if (type == float.class || type == Float.class)
            return Float.parseFloat(valeur);
        if (type == boolean.class || type == Boolean.class)
            return Boolean.parseBoolean(valeur);
        if (type == short.class || type == Short.class)
            return Short.parseShort(valeur);
        if (type == byte.class || type == Byte.class)
            return Byte.parseByte(valeur);
        if (type == char.class || type == Character.class)
            return valeur.charAt(0);
        if (type == LocalDate.class)
            return LocalDate.parse(valeur);                       // yyyy-MM-dd
        if (type == LocalDateTime.class)
            return LocalDateTime.parse(valeur);                   // yyyy-MM-ddTHH:mm
        if (type == Date.class)
            return Date.from(LocalDate.parse(valeur).atStartOfDay(ZoneId.systemDefault()).toInstant());
        if (type == UUID.class)
            return UUID.fromString(valeur);
        if (type.isEnum())
            return Enum.valueOf((Class<Enum>) type, valeur);
        return valeur;
    }

    private static Object construireObjet(Class<?> type, String prefixe, HttpServletRequest request) throws Exception {
        Object instance = type.getDeclaredConstructor().newInstance();
        remplirObjet(instance, type, prefixe, request, 0);
        return instance;
    }

    /**
     * Remplit les champs de l'objet (y compris ceux des classes parentes) depuis la requête.
     * Les objets imbriqués sont créés seulement si au moins un de leurs champs est présent.
     * Retourne true si au moins un champ a été rempli.
     */
    private static boolean remplirObjet(Object instance, Class<?> type, String prefixe,
                                        HttpServletRequest request, int profondeur) throws Exception {
        boolean rempli = false;

        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }

                String nom = prefixe.isEmpty() ? field.getName() : prefixe + "." + field.getName();
                Class<?> typeChamp = field.getType();
                Object valeur = null;

                if (typeChamp == Part.class) {
                    valeur = lirePart(request, nom);
                } else if (typeChamp == MapFile.class) {
                    valeur = lireMapFile(request, nom);
                } else if (typeChamp == MapFile[].class) {
                    MapFile[] fichiers = lireMapFiles(request, nom);
                    valeur = fichiers.length > 0 ? fichiers : null;
                } else if (estTypeSimple(typeChamp)) {
                    String texte = request.getParameter(nom);
                    if (texte != null && !texte.isBlank()) {
                        valeur = convertir(texte, typeChamp);
                    }
                } else if (peutEtreImbrique(typeChamp) && profondeur < PROFONDEUR_MAX) {
                    Object enfant = typeChamp.getDeclaredConstructor().newInstance();
                    if (remplirObjet(enfant, typeChamp, nom, request, profondeur + 1)) {
                        valeur = enfant;
                    }
                }

                if (valeur != null) {
                    field.setAccessible(true);
                    field.set(instance, valeur);
                    rempli = true;
                }
            }
        }
        return rempli;
    }

    private static boolean peutEtreImbrique(Class<?> type) {
        if (type.isPrimitive() || type.isArray() || type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
            return false;
        }
        String nom = type.getName();
        if (nom.startsWith("java.") || nom.startsWith("jakarta.")) {
            return false; // List, Map, ... : pas de binding automatique
        }
        try {
            type.getDeclaredConstructor();
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    // =====================================================================
    //  Upload de fichiers
    // =====================================================================

    private static boolean estMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("multipart/");
    }

    private static boolean contientUnFichier(Part part) {
        String nomFichier = part.getSubmittedFileName();
        return nomFichier != null && !nomFichier.isBlank(); // champ file laissé vide -> pas de fichier
    }

    /**
     * Récupère le fichier envoyé dans le champ "nom" d'un formulaire multipart.
     * Retourne null si la requête n'est pas multipart ou si aucun fichier n'a été choisi.
     */
    private static Part lirePart(HttpServletRequest request, String nom) throws Exception {
        if (!estMultipart(request)) {
            return null;
        }
        Part part = request.getPart(nom);
        return (part != null && contientUnFichier(part)) ? part : null;
    }

    private static MapFile lireMapFile(HttpServletRequest request, String nom) throws Exception {
        Part part = lirePart(request, nom);
        return part == null ? null : versMapFile(part);
    }

    /** Tous les fichiers envoyés sous le même nom de champ (input type="file" multiple). */
    private static MapFile[] lireMapFiles(HttpServletRequest request, String nom) throws Exception {
        List<MapFile> fichiers = new ArrayList<>();
        if (estMultipart(request)) {
            for (Part part : request.getParts()) {
                if (nom.equals(part.getName()) && contientUnFichier(part)) {
                    fichiers.add(versMapFile(part));
                }
            }
        }
        return fichiers.toArray(new MapFile[0]);
    }

    private static MapFile versMapFile(Part part) throws IOException {
        String nomFichier = Paths.get(part.getSubmittedFileName()).getFileName().toString();
        int point = nomFichier.lastIndexOf('.');
        String extension = point >= 0 ? nomFichier.substring(point + 1) : "";
        try (InputStream in = part.getInputStream()) {
            return new MapFile(nomFichier, in.readAllBytes(), extension);
        }
    }

    /**
     * Enregistre le fichier reçu dans le dossier donné (créé si besoin).
     * Le nom d'origine est nettoyé (pas de chemin) et préfixé par l'heure pour éviter les collisions.
     * Retourne le nom sous lequel le fichier a été enregistré.
     */
    public static String sauvegarder(Part part, String dossier) throws IOException {
        String nomOriginal = Paths.get(part.getSubmittedFileName()).getFileName().toString();
        String nomFinal = System.currentTimeMillis() + "_" + nomOriginal;

        Path repertoire = Paths.get(dossier);
        Files.createDirectories(repertoire);

        try (InputStream in = part.getInputStream()) {
            Files.copy(in, repertoire.resolve(nomFinal), StandardCopyOption.REPLACE_EXISTING);
        }
        return nomFinal;
    }

    /** Même chose pour un MapFile (contenu déjà en mémoire). */
    public static String sauvegarder(MapFile fichier, String dossier) throws IOException {
        String nomOriginal = Paths.get(fichier.getFileName()).getFileName().toString();
        String nomFinal = System.currentTimeMillis() + "_" + nomOriginal;

        Path repertoire = Paths.get(dossier);
        Files.createDirectories(repertoire);
        Files.write(repertoire.resolve(nomFinal), fichier.getData());
        return nomFinal;
    }
}
