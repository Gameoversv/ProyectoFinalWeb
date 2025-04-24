package seguridad;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

public class JwtUtil {
    // ✅ Carga la clave desde una variable de entorno para mantener consistencia
    private static final String SECRET = System.getenv().getOrDefault("JWT_SECRET", "defaultSecretKey12345678901234567890");
    private static final Key SECRET_KEY = Keys.hmacShaKeyFor(Base64.getEncoder().encodeToString(SECRET.getBytes()).getBytes());
    private static final long EXPIRATION_TIME = 86400000; // 24 horas

    public static String generarToken(String username, boolean esAdmin) {
        return Jwts.builder()
                .setSubject(username)
                .claim("admin", esAdmin)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY)
                .compact();
    }

    public static boolean validarToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public static String obtenerUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public static boolean esAdmin(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("admin", Boolean.class);
    }
}
