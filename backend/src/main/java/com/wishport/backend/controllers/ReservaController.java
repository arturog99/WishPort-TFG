package com.wishport.backend.controllers;

import com.wishport.backend.dto.ReservaDTO;
import com.wishport.backend.entities.Pista;
import com.wishport.backend.entities.Reserva;
import com.wishport.backend.entities.Usuario;
import com.wishport.backend.repositories.PistaRepository;
import com.wishport.backend.repositories.ReservaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST que gestiona el ciclo completo de las reservas.
 *
 * Rutas base: /api/reservas
 *
 * Endpoints:
 *   GET  /api/reservas                          -> obtenerTodasLasReservas()     [ADMIN]
 *   GET  /api/reservas/hoy                      -> obtenerReservasDeHoy()        [ADMIN]
 *   GET  /api/reservas/usuario/{idUsuario}       -> obtenerReservasPorUsuario()   [privado]
 *   GET  /api/reservas/pista/{id}/fecha/{fecha}  -> obtenerReservasPorPistaYFecha() [público]
 *   GET  /api/reservas/disponibilidad            -> verificarDisponibilidad()     [público]
 *   POST /api/reservas                           -> crearReserva()               [privado]
 *   DELETE /api/reservas/{id}                    -> eliminarReserva()            [privado]
 *
 * La autorización se gestiona con los atributos "idUsuario" y "rol"
 * inyectados por JwtAuthenticationFilter en cada request.
 */
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    /** Repositorio para operaciones CRUD sobre la tabla reservas */
    @Autowired
    private ReservaRepository reservaRepository;

    /** Repositorio para consultar el estado de las pistas */
    @Autowired
    private PistaRepository pistaRepository;

    /**
     * Devuelve TODAS las reservas del sistema.
     * Ruta: GET /api/reservas  [privada - solo ADMIN]
     *
     * Comprueba manualmente que el atributo "rol" del request sea "ADMIN".
     * Si no lo es, lanza AccessDeniedException (Spring Security devuelve 403).
     * Usado en AdminActivity para descargar y filtrar las reservas del día.
     */
    @GetMapping
    public List<Reserva> obtenerTodasLasReservas(HttpServletRequest request) {
        if (!"ADMIN".equals(request.getAttribute("rol"))) {
            throw new org.springframework.security.access.AccessDeniedException("Acceso denegado");
        }
        return reservaRepository.findAll();
    }

    /**
     * Obtiene las reservas del día de hoy (solo accesible para administradores).
     */
    @GetMapping("/hoy")
    public List<Reserva> obtenerReservasDeHoy(HttpServletRequest request) {
        if (!"ADMIN".equals(request.getAttribute("rol"))) {
            throw new org.springframework.security.access.AccessDeniedException("Acceso denegado");
        }
        return reservaRepository.findByFecha(LocalDate.now());
    }

    /**
     * Obtiene todas las reservas de un usuario.
     * Actualiza automáticamente el estado a "completada" si la reserva ya ha pasado.
     */
    @GetMapping("/usuario/{idUsuario}")
    @Transactional
    public List<Reserva> obtenerReservasPorUsuario(@PathVariable Integer idUsuario, HttpServletRequest request) {
        Integer idUsuarioToken = (Integer) request.getAttribute("idUsuario");
        String rol = (String) request.getAttribute("rol");

        // Solo se puede ver las propias reservas o si se es admin
        if (!idUsuario.equals(idUsuarioToken) && !"ADMIN".equals(rol)) {
            throw new org.springframework.security.access.AccessDeniedException("Acceso denegado");
        }

        List<Reserva> reservas = reservaRepository.findByIdUsuario_IdUsuario(idUsuario);
        ZonedDateTime ahora = ZonedDateTime.now(ZoneId.of("Europe/Madrid"));
        boolean actualizar = false;

        // Cambiar estado a 'completada' si la fecha/hora ya ha pasado
        for (Reserva reserva : reservas) {
            if ("activa".equals(reserva.getEstadoReserva())) {
                ZonedDateTime finReserva = ZonedDateTime.of(reserva.getFecha(), reserva.getHoraFin(), ZoneId.of("Europe/Madrid"));
                if (finReserva.isBefore(ahora)) {
                    reserva.setEstadoReserva("completada");
                    actualizar = true;
                }
            }
        }

        if (actualizar) reservaRepository.saveAll(reservas);
        
        return reservas;
    }

    /**
     * Obtiene reservas para una pista y fecha específicas. Útil para el calendario público.
     * Ruta: GET /api/reservas/pista/{idPista}/fecha/{fecha}  [pública - NO requiere JWT]
     *
     * Devuelve ReservaDTO en lugar de la entidad Reserva para no exponer
     * datos privados del usuario (email, teléfono) en un endpoint público.
     */
    @GetMapping("/pista/{idPista}/fecha/{fecha}")
    public List<ReservaDTO> obtenerReservasPorPistaYFecha(
            @PathVariable Integer idPista,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        List<Reserva> reservas = reservaRepository.findByPistaAndFecha(idPista, fecha);
        List<ReservaDTO> dtos = new ArrayList<>();
        for (Reserva r : reservas) {
            dtos.add(new ReservaDTO(r));
        }
        return dtos;
    }

    /**
     * Verifica si una pista está disponible en un horario concreto.
     * Devuelve {disponible: true} si no hay reservas solapadas.
     */
    @GetMapping("/disponibilidad")
    public ResponseEntity<Map<String, Object>> verificarDisponibilidad(
            @RequestParam Integer idPista,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaFin) {

        List<Reserva> solapadas = reservaRepository.findReservasSolapadas(idPista, fecha, horaInicio, horaFin);
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("disponible", solapadas.isEmpty());
        return ResponseEntity.ok(resultado);
    }

    /**
     * Crea una nueva reserva para el usuario autenticado.
     * Ruta: POST /api/reservas  [privada - requiere JWT]
     *
     * Flujo:
     * 1. Extrae el idUsuario del JWT (no se confia en el id que manda el cliente).
     * 2. Verifica que el usuario no tenga ya 2 reservas activas -> 403 LIMITE_ALCANZADO.
     * 3. Verifica que no haya solapamiento de horario -> 409 HORARIO_OCUPADO.
     * 4. Genera el código QR con formato: RSV-{idPista}-{idUsuario}-{timestamp}.
     * 5. Guarda la reserva con estado "activa" -> 201 CREATED.
     *
     * @Transactional asegura que si algo falla, no se guarda nada a medias.
     *
     * @param reserva Objeto JSON con fecha, horaInicio, horaFin e idPista.
     * @param request Request HTTP con el idUsuario inyectado por el filtro JWT.
     * @return 201 CREATED con la reserva creada, o 401/403/409 según el error.
     */
    @PostMapping
    @Transactional
    public ResponseEntity<?> crearReserva(@RequestBody Reserva reserva, HttpServletRequest request) {
        Integer idUsuarioToken = (Integer) request.getAttribute("idUsuario");
        if (idUsuarioToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token no válido");
        }

        // Sobrescribimos el idUsuario con el del token (seguridad: evita asignar reservas a otro usuario)
        reserva.setIdUsuario(new Usuario(idUsuarioToken));

        // Verificar que la pista exista y no esté en mantenimiento
        Pista pista = pistaRepository.findById(reserva.getIdPista().getIdPista()).orElse(null);
        if (pista == null) {
            return error(HttpStatus.NOT_FOUND, "PISTA_NO_ENCONTRADA", "La pista seleccionada no existe en el sistema.");
        }
        if ("mantenimiento".equalsIgnoreCase(pista.getEstado())) {
            return error(HttpStatus.FORBIDDEN, "PISTA_EN_MANTENIMIENTO", "Esta pista está en mantenimiento y no se puede reservar.");
        }

        if (reservaRepository.countByIdUsuario_IdUsuarioAndEstadoReserva(idUsuarioToken, "activa") >= 2) {
            return error(HttpStatus.FORBIDDEN, "LIMITE_ALCANZADO", "Ya tienes 2 reservas activas.");
        }

        List<Reserva> solapadas = reservaRepository.findReservasSolapadas(
                reserva.getIdPista().getIdPista(), reserva.getFecha(), reserva.getHoraInicio(), reserva.getHoraFin()
        );
        if (!solapadas.isEmpty()) {
            return error(HttpStatus.CONFLICT, "HORARIO_OCUPADO", "Este horario ya está reservado");
        }

        // Formato: RSV-{idPista}-{idUsuario}-{milisegundos} -> garantiza unicidad
        reserva.setCodigoQr("RSV-" + reserva.getIdPista().getIdPista() + "-" + idUsuarioToken + "-" + System.currentTimeMillis());
        reserva.setEstadoReserva("activa");

        return ResponseEntity.status(HttpStatus.CREATED).body(reservaRepository.save(reserva));
    }

    /**
     * Cancela (elimina) una reserva existente.
     * Ruta: DELETE /api/reservas/{id}  [privada - requiere JWT]
     *
     * Solo puede cancelarla el dueño de la reserva o un ADMIN.
     * Si otro usuario intenta cancelar una reserva ajena -> 403 FORBIDDEN.
     * Devuelve 204 NO CONTENT al cancelar correctamente (sin cuerpo en la respuesta).
     * Retrofit en Android interpreta el 204 con Callback<Void> como éxito.
     *
     * @param id      ID de la reserva a cancelar.
     * @param request Request HTTP con idUsuario y rol inyectados por el filtro JWT.
     * @return 204 NO CONTENT, 403 FORBIDDEN, o 404 NOT FOUND.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarReserva(@PathVariable Integer id, HttpServletRequest request) {
        Reserva reserva = reservaRepository.findById(id).orElse(null);
        if (reserva == null) {
            return error(HttpStatus.NOT_FOUND, "RESERVA_NO_ENCONTRADA", "La reserva no existe");
        }

        if (!reserva.getIdUsuario().getIdUsuario().equals(request.getAttribute("idUsuario")) && !"ADMIN".equals(request.getAttribute("rol"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No puedes cancelar una reserva ajena");
        }

        // Soft-delete: cambiamos el estado en vez de borrar para mantener historial
        reserva.setEstadoReserva("cancelada");
        reservaRepository.save(reserva);
        return ResponseEntity.noContent().build(); // 204 NO CONTENT
    }

    /**
     * Método auxiliar para construir respuestas de error estructuradas en JSON.
     * Devuelve siempre un objeto con dos campos: "error" (código) y "mensaje" (descripción).
     * Esto permite al frontend identificar el tipo de error (ej: LIMITE_ALCANZADO)
     * y mostrar un mensaje adecuado al usuario.
     *
     * @param status  Código HTTP de la respuesta.
     * @param code    Código interno del error (ej: "HORARIO_OCUPADO").
     * @param message Descripción legible del error.
     */
    private ResponseEntity<?> error(HttpStatus status, String code, String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", code);
        error.put("mensaje", message);
        return ResponseEntity.status(status).body(error);
    }
}
