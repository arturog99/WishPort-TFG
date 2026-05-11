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
 * Filtro de seguridad JWT que se ejecuta una vez por cada petición HTTP.
 *
 * Extiende OncePerRequestFilter para garantizar que la lógica de autenticación
 * se ejecuta exactamente una vez por petición, evitando duplicados.
 *
 * Posición en la cadena: se inserta ANTES de UsernamePasswordAuthenticationFilter
 * (ver SecurityConfig.addFilterBefore), por lo que actúa antes de cualquier
 * autenticación basada en formulario de Spring Security.
 *
 * Flujo para una petición con token válido:
 *   1. Extrae el header "Authorization: Bearer {token}".
 *   2. Valida la firma y comprueba que no ha expirado.
 *   3. Extrae email, idUsuario y rol del payload del token.
 *   4. Crea un objeto de autenticación y lo registra en el SecurityContext.
 *   5. Inyecta idUsuario y rol como atributos de la request para los controladores.
 *   6. Pasa la petición al siguiente filtro / controlador.
 *
 * Si el token es inválido o no está presente, simplemente no autentica
 * y deja que Spring Security rechace la petición si la ruta es privada.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** JwtUtil inyectado para validar y extraer datos del token */
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Lógica principal del filtro. Se ejecuta para cada petición HTTP entrante.
     *
     * @param request     Petición HTTP entrante.
     * @param response    Respuesta HTTP.
     * @param filterChain Cadena de filtros para pasar al siguiente eslabón.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Solo procesamos si existe el header y empieza con "Bearer "
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // Eliminamos el prefijo "Bearer "

            if (jwtUtil.esTokenValido(token) && !jwtUtil.estaExpirado(token)) {
                String email = jwtUtil.extraerEmail(token);
                Integer idUsuario = jwtUtil.extraerIdUsuario(token);
                String rol = jwtUtil.extraerRol(token);

                // Marcamos al usuario como autenticado en el contexto de Spring Security
                // Collections.emptyList() = sin authorities específicas (los permisos los gestionamos con "rol")
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                // Inyectamos idUsuario y rol como atributos accesibles desde los controladores:
                // request.getAttribute("idUsuario") y request.getAttribute("rol")
                request.setAttribute("idUsuario", idUsuario);
                request.setAttribute("rol", rol);
            }
        }

        // Pasamos la petición al siguiente filtro o al controlador destino
        filterChain.doFilter(request, response);
    }
}
