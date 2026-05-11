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
 *
 * Un JWT (JSON Web Token) tiene 3 partes separadas por puntos:
 *   HEADER.PAYLOAD.SIGNATURE
 *
 * En esta app el PAYLOAD (claims) contiene:
 *   - subject: email del usuario
 *   - idUsuario: ID numérico del usuario
 *   - rol: "USER" o "ADMIN"
 *   - iat: fecha de creación (issued at)
 *   - exp: fecha de expiración (24h después de iat)
 *
 * La SIGNATURE garantiza que nadie puede modificar el token sin invalidarlo.
 * Se firma con HMAC-SHA256 usando la clave secreta KEY.
 *
 * Flujo típico:
 *   1. login() llama a generarToken() -> devuelve el token al cliente.
 *   2. El cliente lo guarda y lo envía en cada petición en el header "Authorization: Bearer {token}".
 *   3. JwtAuthenticationFilter llama a esTokenValido() y estaExpirado() para validarlo.
 *   4. Si es válido, extrae idUsuario y rol con extraerIdUsuario() y extraerRol().
 */
@Component
public class JwtUtil {

    /**
     * Clave secreta para firmar y verificar los tokens con HMAC-SHA256.
     * IMPORTANTE: En producción debe estar en una variable de entorno,
     * nunca hardcodeada en el código fuente.
     */
    private static final String SECRET = "wishport-tfg-super-secreto-2026-no-compartir-jamas";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes());

    /** Tiempo de vida del token: 24 horas en milisegundos (86400 segundos * 1000) */
    private static final long EXPIRATION_MS = 86400000;

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

    /**
     * Extrae el email del usuario (guardado como "subject" del token).
     * @param token Token JWT válido.
     * @return Email del usuario.
     */
    public String extraerEmail(String token) {
        return extraerClaims(token).getSubject();
    }

    /**
     * Extrae el ID numérico del usuario del payload del token.
     * @param token Token JWT válido.
     * @return ID del usuario.
     */
    public Integer extraerIdUsuario(String token) {
        return (Integer) extraerClaims(token).get("idUsuario");
    }

    /**
     * Extrae el rol del usuario del payload del token ("USER" o "ADMIN").
     * @param token Token JWT válido.
     * @return Rol del usuario.
     */
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
