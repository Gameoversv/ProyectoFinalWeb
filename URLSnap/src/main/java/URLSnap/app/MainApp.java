package URLSnap.app;

import config.MongoConfig;
import dao.UserDao;
import dao.UrlDao;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import modelo.User;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import io.javalin.rendering.template.JavalinThymeleaf;
import seguridad.JwtUtil;

import java.io.File;
import java.util.Date;
import java.util.Map;

public class MainApp {

    public static UserDao userDAO;
    public static UrlDao urlDAO;

    public static void initDAOs() {
        MongoConfig.init();
        userDAO = new UserDao();
        urlDAO = new UrlDao();

        if (userDAO.getUserByUsername("admin") == null) {
            User admin = new User();
            admin.setNombre("Administrador");
            admin.setUsername("admin");
            admin.setPassword("admin");
            admin.setAdmin(true);
            admin.setCreatedAt(new Date());
            userDAO.addUser(admin);
            System.out.println("👑 Usuario administrador creado.");
        }
    }

    public static void main(String[] args) {
        System.out.println("💥 MainApp ejecutándose correctamente ✅");

        initDAOs();

        TemplateEngine templateEngine = configurarThymeleaf();
        Javalin app = iniciarJavalin(templateEngine);
        imprimirTemplatesDisponibles();
        configurarRutas(app);

        System.out.println("🚀 Servidor disponible en http://localhost:7000/");
    }

    private static Javalin iniciarJavalin(TemplateEngine templateEngine) {
        return Javalin.create(config -> {
            config.staticFiles.add(staticFiles -> {
                staticFiles.directory = "/public";
                staticFiles.hostedPath = "/";
                staticFiles.location = Location.CLASSPATH;
                staticFiles.headers.put("Service-Worker-Allowed", "/");
            });
            config.fileRenderer(new JavalinThymeleaf(templateEngine));
        }).start(7000);
    }

    private static TemplateEngine configurarThymeleaf() {
        TemplateEngine templateEngine = new TemplateEngine();
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("/templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        templateEngine.setTemplateResolver(resolver);
        return templateEngine;
    }

    private static void imprimirTemplatesDisponibles() {
        File templateDir = new File("build/resources/main/templates");
        System.out.println("📁 Templates encontrados:");
        if (templateDir.exists()) {
            for (File file : templateDir.listFiles()) {
                System.out.println("📝 " + file.getName());
            }
        } else {
            System.out.println("❌ No se encontraron templates.");
        }
    }

    private static void configurarRutas(Javalin app) {
        configurarMiddleware(app);
        configurarRutasAutenticacion(app);
        configurarRutasPublicas(app);
        configurarRutasPrivadas(app);
    }

    private static void configurarMiddleware(Javalin app) {
        app.before(ctx -> {
            String token = ctx.cookie("token");

            boolean rutaPublica = ctx.path().startsWith("/login") ||
                    ctx.path().startsWith("/register") ||
                    ctx.path().startsWith("/index") ||
                    ctx.path().startsWith("/partials") ||
                    ctx.path().startsWith("/components") ||
                    ctx.path().startsWith("/logout") ||
                    ctx.path().startsWith("/css") ||
                    ctx.path().startsWith("/js") ||
                    ctx.path().startsWith("/public") ||
                    ctx.path().startsWith("/service-worker.js") ||
                    ctx.path().equals("/") ||
                    ctx.path().equals("/api/acortar") ||
                    ctx.path().equals("/api/mis-urlsnaps") ||
                    ctx.path().startsWith("/api/yo");

            // ⬇️ Si es ruta pública y no hay token, salir
            if (rutaPublica && token == null) return;

            if (token == null || !JwtUtil.validarToken(token)) {
                ctx.status(401).json(Map.of("error", "Token inválido o ausente"));
                ctx.result();
                return;
            }

            String username = JwtUtil.obtenerUsername(token);
            boolean esAdmin = JwtUtil.esAdmin(token);
            User user = userDAO.getUserByUsername(username);

            if (user == null) {
                ctx.status(401).json(Map.of("error", "Usuario no encontrado"));
                return;
            }

            ctx.attribute("user", user);
            ctx.attribute("esAdmin", esAdmin);
        });
    }



    private static void configurarRutasAutenticacion(Javalin app) {
        app.get("/index", controladores.AuthControlador::mostrarIndex);
        app.get("/login", controladores.AuthControlador::mostrarLogin);
        app.post("/login", controladores.AuthControlador::procesarLogin);
        app.post("/logout", controladores.AuthControlador::procesarLogout);
        app.post("/register", controladores.AuthControlador::procesarRegistro);
    }

    private static void configurarRutasPublicas(Javalin app) {
        app.get("/", ctx -> ctx.redirect("/index"));

        app.post("/api/acortar", controladores.UrlController::acortarUrl);
        app.get("/api/mis-urlsnaps", controladores.ApiControlador::obtenerUrls);
        app.get("/api/mis-urlsnaps/detalles", controladores.ApiControlador::obtenerUrlsConDetalles);
        app.get("/api/yo", controladores.ApiControlador::obtenerUsuarioActual);
        app.get("/api/estadisticas/{shortCode}", controladores.ApiControlador::obtenerEstadisticas);
        app.delete("/api/url/{shortCode}", controladores.UrlController::eliminarUrl);

        app.get("/mis-urlsnaps", ctx -> ctx.render("mis-urlsnaps.html"));
        app.get("/mis-urlsnapsadmin", ctx -> ctx.render("mis-urlsnapsadmin.html"));
        app.get("/usuarios", ctx -> ctx.render("usuarios.html"));
        app.get("/estadisticas", ctx -> ctx.render("estadisticas.html"));

        app.get("/service-worker.js", ctx -> {
            ctx.contentType("application/javascript");
            ctx.result(MainApp.class.getResourceAsStream("/public/service-worker.js"));
        });

        app.get("/{shortCode}", ctx -> {
            String shortCode = ctx.pathParam("shortCode");
            if (shortCode.matches("^[\\w]{5,20}$")) {
                controladores.UrlController.redirigirUrl(ctx);
            } else {
                ctx.status(404).result("❌ Ruta no válida");
            }
        });
    }

    private static void configurarRutasPrivadas(Javalin app) {
        app.before("/api/todas-las-urls", ctx -> {
            Boolean esAdmin = ctx.attribute("esAdmin");
            if (esAdmin == null || !esAdmin) {
                ctx.status(403).json(Map.of("error", "Acceso restringido a administradores"));
            }
        });

        app.get("/api/todas-las-urls", controladores.ApiControlador::obtenerTodasLasUrls);
        app.get("/api/usuarios", controladores.ApiControlador::obtenerUsuarios);
        app.delete("/api/usuarios/{id}", controladores.ApiControlador::eliminarUsuario);
    }
}
