package com.wishport.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


@Component
public class JwtUtil {

    // Clave segura de 256 bits (deberías mover esto a una variable de entorno en producción)
    private static final String SECRET = "wishport-tfg-super-secreto-2026-no-compartir-jamas";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes());

    private static final long EXPIRATION_MS = 86400000; // 24 horas

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

    public boolean esTokenValido(String token) {
        try {
            extraerClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean estaExpirado(String token) {
        return extraerClaims(token).getExpiration().before(new Date());
    }
}
