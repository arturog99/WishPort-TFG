package com.wishport.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilidad para generar, validar y extraer información de tokens JWT.
 */
@Component
public class JwtUtil {

    // Clave secreta para firmar y verificar los tokens.
    // En un entorno real, debería estar en una variable de entorno.
    private static final String SECRET = "wishport-tfg-super-secreto-2026-no-compartir-jamas";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes());

    private static final long EXPIRATION_MS = 86400000; // 24 horas

    /**
     * Genera un nuevo token JWT para un usuario.
     * @param idUsuario ID del usuario.
     * @param email Email del usuario (se usa como "subject" del token).
     * @param rol Rol del usuario (para control de acceso).
     * @return El token JWT como una cadena de texto.
     */
    public String generarToken(Integer idUsuario, String email, String rol) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("idUsuario", idUsuario);
        claims.put("rol", rol);

        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(KEY)
                .compact();
    }

    /**
     * Extrae todos los "claims" (datos) de un token.
     * Si el token es inválido o ha expirado, lanzará una excepción.
     */
    public Claims extraerClaims(String token) {
        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extraerEmail(String token) {
        return extraerClaims(token).getSubject();
    }

    public Integer extraerIdUsuario(String token) {
        return (Integer) extraerClaims(token).get("idUsuario");
    }

    public String extraerRol(String token) {
        return (String) extraerClaims(token).get("rol");
    }

    /**
     * Comprueba si un token es válido (firma correcta y formato correcto).
     */
    public boolean esTokenValido(String token) {
        try {
            extraerClaims(token);
            return true;
        } catch (Exception e) {
            return false; // El token es inválido si falla la extracción
        }
    }

    /**
     * Comprueba si un token ha expirado.
     */
    public boolean estaExpirado(String token) {
        return extraerClaims(token).getExpiration().before(new Date());
    }
}
