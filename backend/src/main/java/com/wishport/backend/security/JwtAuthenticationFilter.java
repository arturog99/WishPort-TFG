package com.wishport.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Filtro que se ejecuta una vez por cada petición HTTP.
 * Se encarga de interceptar la petición, buscar un token JWT en las cabeceras,
 * validarlo y autorizar al usuario en el sistema.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Buscar el token en la cabecera "Authorization"
        String authHeader = request.getHeader("Authorization");

        // El token válido debe empezar con "Bearer "
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // Si el token no ha sido alterado y no ha caducado
            if (jwtUtil.esTokenValido(token) && !jwtUtil.estaExpirado(token)) {
                String email = jwtUtil.extraerEmail(token);
                Integer idUsuario = jwtUtil.extraerIdUsuario(token);
                String rol = jwtUtil.extraerRol(token);

                // Autenticamos al usuario dentro de Spring Security
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                // Inyectamos datos extra en la petición para poder usarlos en los Controladores
                request.setAttribute("idUsuario", idUsuario);
                request.setAttribute("rol", rol);
            }
        }

        // Continuar con el resto de filtros (si los hay) o ir al Controlador
        filterChain.doFilter(request, response);
    }
}
