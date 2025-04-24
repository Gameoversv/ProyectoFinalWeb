package controladores;

import URLSnap.app.MainApp;
import io.javalin.http.Context;
import modelo.Stats;
import modelo.Url;
import modelo.User;
import seguridad.JwtUtil;
import util.PreviewUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.util.*;

public class UrlController {

    public static void acortarUrl(Context ctx) {
        try {
            Map<String, String> body = ctx.bodyAsClass(Map.class);
            String urlOriginal = body.get("url");

            if (urlOriginal == null || urlOriginal.trim().isEmpty()) {
                ctx.status(400).json(Map.of("error", "URL vacía"));
                return;
            }

            String userId = obtenerUserIdDesdeSesion(ctx);
            Url yaExiste = MainApp.urlDAO.getUrlByOriginalUrlAndUserId(urlOriginal, userId);

            if (yaExiste != null) {
                ctx.json(Map.of(
                        "originalUrl", yaExiste.getOriginalUrl(),
                        "shortUrl", generarShortUrl(yaExiste.getShortCode()),
                        "previewBase64", yaExiste.getPreviewImage()
                ));
                return;
            }

            String shortCode = generarCodigoUnico();
            Url nuevaUrl = new Url();
            nuevaUrl.setOriginalUrl(urlOriginal);
            nuevaUrl.setShortCode(shortCode);
            nuevaUrl.setUserId(userId);
            nuevaUrl.setCreatedAt(new Date());
            nuevaUrl.setStats(new ArrayList<>());

            // 🔍 Obtener imagen preview
            String previewBase64 = PreviewUtil.fetchPreviewImageBase64(urlOriginal);
            nuevaUrl.setPreviewImage(previewBase64);

            MainApp.urlDAO.addUrl(nuevaUrl);

            ctx.json(Map.of(
                    "originalUrl", urlOriginal,
                    "shortUrl", generarShortUrl(shortCode),
                    "previewBase64", previewBase64
            ));

        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    public static void redirigirUrl(Context ctx) {
        String shortCode = ctx.pathParam("shortCode");
        Url url = MainApp.urlDAO.getUrlByShortCode(shortCode);

        if (url != null) {
            // 📡 IP normalizada
            String ip = ctx.ip();
            if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
                ip = "127.0.0.1";
            }

            // 🌍 Buscar país vía IP
            String pais = obtenerPaisDesdeIP(ip);

            Stats stats = new Stats(
                    new Date(),
                    ip,
                    ctx.userAgent(),
                    getOperatingSystem(ctx.userAgent()),
                    ctx.header("Referer"),
                    pais
            );

            MainApp.urlDAO.agregarEstadistica(shortCode, stats);
            ctx.redirect(url.getOriginalUrl());
        } else {
            ctx.status(404).json(Map.of("error", "URL no encontrada"));
        }
    }

    public static void eliminarUrl(Context ctx) {
        String shortCode = ctx.pathParam("shortCode");
        String userId = obtenerUserIdDesdeSesion(ctx);

        boolean eliminado = MainApp.urlDAO.deleteUrlByShortCodeAndUser(shortCode, userId);
        if (eliminado) {
            ctx.status(200).json(Map.of("mensaje", "URL eliminada correctamente"));
        } else {
            ctx.status(404).json(Map.of("error", "URL no encontrada o no autorizada"));
        }
    }

    // 🌐 Obtener país desde API pública
    private static String obtenerPaisDesdeIP(String ip) {
        if ("127.0.0.1".equals(ip)) return "Localhost";

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ipapi.co/" + ip + "/country_name/"))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body().trim();
        } catch (Exception e) {
            System.err.println("❌ Error al obtener país desde IP: " + e.getMessage());
            return "Desconocido";
        }
    }

    private static String generarCodigoUnico() {
        String base62 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        String shortCode;
        do {
            StringBuilder hash = new StringBuilder();
            try {
                String input = System.nanoTime() + "-" + UUID.randomUUID();
                byte[] bytes = MessageDigest.getInstance("MD5").digest(input.getBytes());
                for (int i = 0; i < 6; i++) {
                    hash.append(base62.charAt((bytes[i] & 0xFF) % 62));
                }
            } catch (Exception e) {
                throw new RuntimeException("Error generando código", e);
            }
            shortCode = hash.toString();
        } while (MainApp.urlDAO.getUrlByShortCode(shortCode) != null);
        return shortCode;
    }

    private static String getOperatingSystem(String userAgent) {
        if (userAgent == null) return "Desconocido";
        String ua = userAgent.toLowerCase();
        if (ua.contains("windows")) return "Windows";
        if (ua.contains("mac")) return "Mac";
        if (ua.contains("linux")) return "Linux";
        if (ua.contains("android")) return "Android";
        if (ua.contains("iphone")) return "iOS";
        return "Otro";
    }

    private static String obtenerUserIdDesdeSesion(Context ctx) {
        String token = ctx.cookie("token");
        if (token != null && JwtUtil.validarToken(token)) {
            String username = JwtUtil.obtenerUsername(token);
            User user = MainApp.userDAO.getUserByUsername(username);
            if (user != null) return user.getId().toString();
        }

        String anonId = ctx.sessionAttribute("anonUserId");
        if (anonId == null) {
            anonId = UUID.randomUUID().toString();
            ctx.sessionAttribute("anonUserId", anonId);
        }
        return anonId;
    }


    private static String generarShortUrl(String shortCode) {
        return "http://localhost:7000/" + shortCode;
    }
}
