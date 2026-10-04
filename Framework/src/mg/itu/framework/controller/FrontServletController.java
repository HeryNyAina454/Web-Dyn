package mg.itu.framework.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mg.itu.framework.dto.MethodDTO;
import mg.itu.framework.dto.RequestMapping;
import mg.itu.framework.listener.FrameworkInitializerListener;
import mg.itu.framework.utils.Utilitaire;
import mg.itu.framework.vue.ViewResolver;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class FrontServletController extends HttpServlet {

    private Utilitaire utilitaire;
    private List<Class<?>> listeController = new ArrayList<>();
    private Map<RequestMapping, MethodDTO> mapMethode = new LinkedHashMap<>();
    private ViewResolver viewResolver;

    @Override
    @SuppressWarnings("unchecked")
    public void init() {
        // --- Map de routage ---
        Object mapPubliee = getServletContext().getAttribute(FrameworkInitializerListener.MAPPING_ATTRIBUTE);
        if (mapPubliee instanceof Map) {
            mapMethode.putAll((Map<RequestMapping, MethodDTO>) mapPubliee);
        } else {
            // fallback : listener non actif
            utilitaire = new Utilitaire();
            List<String> packagesName = Utilitaire.parsePackages(getInitParameter("base-packages"));
            if (packagesName.isEmpty()) {
                listeController.addAll(utilitaire.findController(utilitaire.chargerClassePath("all")));
            } else {
                for (String pkg : packagesName) {
                    listeController.addAll(utilitaire.findController(utilitaire.chargerClassePath(pkg)));
                }
            }
            mapMethode.putAll(utilitaire.findMethod(listeController));
        }

        // --- ViewResolver ---
        Object vrPublie = getServletContext().getAttribute(FrameworkInitializerListener.VIEW_RESOLVER_ATTRIBUTE);
        if (vrPublie instanceof ViewResolver) {
            viewResolver = (ViewResolver) vrPublie;
        } else {
            // fallback : valeurs par défaut
            viewResolver = new ViewResolver("/WEB-INF/views/", ".jsp");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        handleRequest(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        handleRequest(req, res);
    }

    private void handleRequest(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String url = req.getServletPath();
        if (url == null || url.trim().isEmpty()) {
            url = "/";
        }
        String httpMethod = req.getMethod();

        RequestMapping demande = new RequestMapping(url, httpMethod);
        MethodDTO trouve = mapMethode.get(demande);

        if (trouve != null) {
            try {
                Object result = Utilitaire.invokeMethod(trouve);
                Utilitaire.render(result, viewResolver, req, res);
            } catch (Exception e) {
                throw new ServletException("Erreur lors de l'invocation de " + trouve, e);
            }
        } else {
            res.setContentType("text/html;charset=UTF-8");
            try (PrintWriter out = res.getWriter()) {
                out.println("<h4>Erreur 404</h4>");
                out.println("<p>Url inconnue : \"" + url + "\" [" + httpMethod + "]</p>");
                out.println("<h5>Routes disponibles :</h5><ul>");
                for (Map.Entry<RequestMapping, MethodDTO> entry : mapMethode.entrySet()) {
                    out.println("<li>" + entry.getKey().getUrl()
                            + " [" + entry.getKey().getMethod() + "] → "
                            + entry.getValue() + "</li>");
                }
                out.println("</ul>");
            }
        }
    }
}