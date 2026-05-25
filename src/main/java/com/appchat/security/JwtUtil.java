package com.appchat.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JwtUtil {

    private static final Logger LOGGER = Logger.getLogger(JwtUtil.class.getName());
    private static final String JWT_SECRET_ENV = System.getenv("JWT_SECRET");
    private static final Key KEY;

    static {
        String secret = JWT_SECRET_ENV;
        // Allow JVM system property as an alternative (useful for -DJWT_SECRET=...)
        if (secret == null || secret.trim().isEmpty()) {
            secret = System.getProperty("JWT_SECRET");
        }
        // Fallback explícito solicitado para desarrollo local
        if (secret == null || secret.trim().isEmpty()) {
            secret = "loqueseaperoquetenga32caracteres";
            LOGGER.log(Level.WARNING, "Variable de entorno JWT_SECRET no encontrada. Usando valor por defecto para desarrollo.");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres");
        }
        KEY = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public static Key getKey() {
        return KEY;
    }

    private static Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public static String generarToken(Long userId, String email) {
        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(KEY)
                .compact();
    }

    public static boolean esTokenValido(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String obtenerEmail(String token) {
        return parse(token).getSubject();
    }

    public static Long getUserIdFromToken(String token) {
        return parse(token).get("userId", Long.class);
    }
}