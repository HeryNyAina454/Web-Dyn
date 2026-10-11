package com.framework.service;

import com.framework.annotation.Controller;
import com.framework.annotation.FrontMapping;
import com.framework.annotation.Url;
import com.framework.model.UrlMapping;
import com.framework.model.UrlMethod;
import com.framework.model.VueData;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
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
}

