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
 * Configuración central de Spring Security para la API WishPort.
 *
 * Define:
 *   1. El encriptador de contraseñas (BCrypt).
 *   2. Las reglas de acceso a las rutas (públicas vs privadas).
 *   3. La integración del filtro JWT en la cadena de seguridad.
 *
 * Principios de seguridad aplicados:
 *   - Sin estado (STATELESS): no se usan sesiones HTTP, cada petición
 *     debe autenticarse por sí sola con el token JWT.
 *   - CSRF desactivado: no necesario en APIs REST sin estado con JWT.
 */
@Configuration
public class SecurityConfig {

    /** Filtro JWT inyectado para añadirlo a la cadena de seguridad */
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Define el encriptador de contraseñas como Bean de Spring.
     * BCrypt es un algoritmo de hashing adaptativo: es lento a propósito
     * para dificultar ataques de fuerza bruta.
     * Se inyecta en UsuarioController para hashear al registrar
     * y para comparar al hacer login.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configura la cadena de filtros de seguridad HTTP.
     *
     * Rutas PÚBLICAS (sin token):
     *   - POST /api/usuarios          -> Registro
     *   - POST /api/usuarios/login    -> Login
     *   - GET  /api/pistas            -> Lista de pistas
     *   - GET  /api/reservas/disponibilidad -> Verificar disponibilidad
     *   - GET  /api/reservas/pista/*/fecha/* -> Reservas de una pista por fecha
     *   - GET  /images/**             -> Imágenes estáticas
     *
     * Rutas PRIVADAS (requieren JWT válido): cualquier otra ruta.
     *
     * @param http Objeto de configuración de Spring Security.
     * @return La cadena de filtros construida.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // Sin CSRF en API REST sin estado
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/usuarios/login", "/api/usuarios", "/api/pistas",
                                 "/api/reservas/disponibilidad", "/api/reservas/pista/*/fecha/*", "/images/**")
                .permitAll()
                .anyRequest().authenticated()
            )
            // El filtro JWT se ejecuta antes del filtro de autenticación por formulario
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
