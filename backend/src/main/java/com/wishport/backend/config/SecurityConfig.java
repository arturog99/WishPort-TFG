package com.wishport.backend.config;

import com.wishport.backend.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de seguridad de Spring Security.
 * Define qué rutas son públicas, cuáles privadas y añade el filtro JWT.
 */
@Configuration
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Encriptador para hashear las contraseñas antes de guardarlas en base de datos.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Cadena de filtros de seguridad.
     * Desactiva sesiones (usa tokens) y define permisos por ruta.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Rutas públicas (no requieren token)
                .requestMatchers("/api/usuarios/login", "/api/usuarios", "/api/pistas", 
                                 "/api/reservas/disponibilidad", "/api/reservas/pista/*/fecha/*", "/images/**")
                .permitAll()
                
                // Rutas privadas (cualquier otra ruta requiere token válido)
                .anyRequest().authenticated()
            )
            // Añadir filtro que intercepta cada petición y valida el token JWT
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
