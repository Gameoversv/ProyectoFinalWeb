package controladores;

import URLSnap.app.MainApp;
import io.javalin.http.Context;
import io.javalin.http.Cookie;
import modelo.User;
import seguridad.JwtUtil;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AuthControlador {

    public static void mostrarLogin(Context ctx) {
        User usuario = ctx.sessionAttribute("user");
        if (usuario != null) {
            ctx.redirect("/index");
            return;
        }
        ctx.render("login.html");
    }

    public static void mostrarIndex(Context ctx) {
        User usuario = ctx.sessionAttribute("user");
        Map<String, Object> model = new HashMap<>();
        if (usuario != null) {
            model.put("usuario", usuario);
        }
        ctx.render("index.html", model);
    }

    public static void procesarLogin(Context ctx) {
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");

        User usuario = MainApp.userDAO.getUserByUsername(username);
        if (usuario != null && usuario.getPassword().equals(password)) {
            String token = JwtUtil.generarToken(usuario.getUsername(), usuario.isAdmin());

            // ✅ Cookie personalizada con path=/ y httpOnly=false
            Cookie cookie = new Cookie("token", token);
            cookie.setPath("/");
            cookie.setHttpOnly(false);
            cookie.setMaxAge(86400); // 1 día

            ctx.cookie(cookie);
            ctx.json(Map.of("token", token, "username", usuario.getUsername()));
        } else {
            ctx.status(401).json(Map.of("error", "Credenciales inválidas"));
        }
    }

    public static void procesarRegistro(Context ctx) {
        String nombre = ctx.formParam("fullname");
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");

        if (MainApp.userDAO.getUserByUsername(username) != null) {
            ctx.status(400).json(Map.of("error", "Usuario ya existe"));
            return;
        }

        User nuevoUsuario = new User();
        nuevoUsuario.setNombre(nombre);
        nuevoUsuario.setUsername(username);
        nuevoUsuario.setPassword(password);
        nuevoUsuario.setAdmin(false);
        nuevoUsuario.setCreatedAt(new Date());

        MainApp.userDAO.addUser(nuevoUsuario);
        ctx.status(201).json(Map.of("mensaje", "Usuario registrado"));
    }

    public static void procesarLogout(Context ctx) {
        ctx.req().getSession().invalidate();
        ctx.removeCookie("token", "/");
        ctx.json(Map.of("mensaje", "Logout exitoso")); // ✅ en vez de redirect
    }

}
