package controladores;

import URLSnap.app.MainApp;
import io.javalin.http.Context;
import modelo.User;
import modelo.Url;
import org.bson.types.ObjectId;
import seguridad.JwtUtil;

import java.util.*;
import java.util.stream.Collectors;

public class ApiControlador {

    public static void obtenerUsuarios(Context ctx) {
        String token = ctx.cookie("token");
        if (token == null || !JwtUtil.validarToken(token)) {
            ctx.status(401).json(Map.of("error", "No autenticado"));
            return;
        }

        boolean esAdmin = JwtUtil.esAdmin(token);
        if (!esAdmin) {
            ctx.status(403).json(Map.of("error", "Acceso no autorizado"));
            return;
        }

        List<User> usuarios = MainApp.userDAO.getAllUsers();
        List<Map<String, Object>> usuariosDTO = new ArrayList<>();

        for (User user : usuarios) {
            Map<String, Object> userMap = new LinkedHashMap<>();
            userMap.put("id", user.getId().toString());
            userMap.put("nombre", user.getNombre());
            userMap.put("username", user.getUsername());
            userMap.put("esAdmin", user.isAdmin());
            usuariosDTO.add(userMap);
        }

        ctx.json(usuariosDTO);
    }


    public static void eliminarUsuario(Context ctx) {
        String idStr = ctx.pathParam("id");
        User user = ctx.attribute("user");

        try {
            ObjectId idAEliminar = new ObjectId(idStr);

            if (user == null || !user.isAdmin()) {
                ctx.status(403).json(Map.of("error", "No autorizado"));
                return;
            }

            if (user.getId().equals(idAEliminar)) {
                ctx.status(400).json(Map.of("error", "No te puedes eliminar a ti mismo"));
                return;
            }

            boolean eliminado = MainApp.userDAO.deleteUserById(idAEliminar);
            if (eliminado) {
                ctx.status(200).json(Map.of("mensaje", "Usuario eliminado"));
            } else {
                ctx.status(404).json(Map.of("error", "Usuario no encontrado"));
            }
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(Map.of("error", "ID inválido"));
        }
    }

    public static void obtenerUrls(Context ctx) {
        User user = ctx.attribute("user");
        String userId;

        if (user != null) {
            userId = user.getId().toString();
        } else {
            userId = ctx.sessionAttribute("anonUserId");
            if (userId == null) {
                userId = UUID.randomUUID().toString();
                ctx.sessionAttribute("anonUserId", userId); // 🧠 Guarda ID anónimo
            }
        }

        List<Url> urls = MainApp.urlDAO.getUrlsByUserId(userId);
        List<Map<String, Object>> urlsDTO = new ArrayList<>();

        for (Url url : urls) {
            Map<String, Object> urlMap = new LinkedHashMap<>();
            urlMap.put("originalUrl", url.getOriginalUrl());
            urlMap.put("shortCode", url.getShortCode());
            urlMap.put("createdAt", url.getCreatedAt());
            urlsDTO.add(urlMap);
        }

        ctx.json(urlsDTO);
    }



    public static void obtenerUrlsConDetalles(Context ctx) {
        User user = ctx.attribute("user");
        String userId = user != null ? user.getId().toString() : null;

        List<Url> urls = MainApp.urlDAO.getUrlsByUserId(userId);
        List<Map<String, Object>> urlsDTO = urls.stream().map(url -> Map.of(
                "originalUrl", url.getOriginalUrl(),
                "shortUrl", "http://localhost:7000/" + url.getShortCode(),
                "createdAt", url.getCreatedAt(),
                "stats", url.getStats(),
                "previewBase64", url.getPreviewImage()
        )).collect(Collectors.toList());

        ctx.json(urlsDTO);
    }

    public static void obtenerUsuarioActual(Context ctx) {
        User user = ctx.attribute("user");
        if (user != null) {
            ctx.json(Map.of(
                    "username", user.getUsername(),
                    "esAdmin", user.isAdmin()
            ));
        } else {
            ctx.status(401).json(Map.of("error", "No autenticado"));
        }
    }

    public static void obtenerTodasLasUrls(Context ctx) {
        User user = ctx.attribute("user");
        if (user == null || !user.isAdmin()) {
            ctx.status(403).json(Map.of("error", "No autorizado"));
            return;
        }

        List<Url> urls = MainApp.urlDAO.getAllUrls();
        List<Map<String, Object>> urlsDTO = urls.stream().map(url -> {
            Map<String, Object> urlMap = new LinkedHashMap<>();
            urlMap.put("id", url.getId().toString());
            urlMap.put("originalUrl", url.getOriginalUrl());
            urlMap.put("shortCode", url.getShortCode());
            urlMap.put("createdAt", url.getCreatedAt());

            String userId = url.getUserId();
            User creador = null;

            if (userId != null && ObjectId.isValid(userId)) {
                creador = MainApp.userDAO.getUserById(new ObjectId(userId));
            }

            urlMap.put("username", creador != null ? creador.getUsername() : "Anónimo");
            return urlMap;
        }).collect(Collectors.toList());

        ctx.json(urlsDTO);
    }

    public static void obtenerEstadisticas(Context ctx) {
        String shortCode = ctx.pathParam("shortCode");
        Url url = MainApp.urlDAO.getUrlByShortCode(shortCode);

        if (url == null) {
            ctx.status(404).json(Map.of("error", "URL no encontrada"));
            return;
        }

        ctx.json(url.getStats());
    }
    public static void obtenerUsuarioPorId(Context ctx) {
        String id = ctx.pathParam("id");
        User user = MainApp.userDAO.getById(id);
        if (user == null) {
            ctx.status(404).json(Map.of("error", "Usuario no encontrado"));
            return;
        }
        ctx.json(Map.of(
                "id", user.getId().toString(),
                "nombre", user.getNombre(),
                "username", user.getUsername(),
                "esAdmin", user.isAdmin()
        ));
    }

    public static void actualizarUsuario(Context ctx) {
        String id = ctx.pathParam("id");
        User existente = MainApp.userDAO.getById(id);
        if (existente == null) {
            ctx.status(404).json(Map.of("error", "Usuario no encontrado"));
            return;
        }

        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        String nombre = (String) body.get("nombre");
        String username = (String) body.get("username");
        boolean esAdmin = (boolean) body.get("admin");

        existente.setNombre(nombre);
        existente.setUsername(username);
        existente.setAdmin(esAdmin);

        MainApp.userDAO.update(existente);
        ctx.json(Map.of("mensaje", "Usuario actualizado correctamente"));
    }

}
