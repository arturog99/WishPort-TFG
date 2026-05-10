package com.wishport.backend.controllers;

import com.wishport.backend.entities.Reserva;
import com.wishport.backend.entities.Usuario;
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
import java.util.List;
import java.util.Map;

/**
 * Controlador para gestionar las operaciones de reservas de pistas.
 */
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    @Autowired
    private ReservaRepository reservaRepository;

    /**
     * Obtiene todas las reservas (solo accesible para administradores).
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
     */
    @GetMapping("/pista/{idPista}/fecha/{fecha}")
    public List<Reserva> obtenerReservasPorPistaYFecha(
            @PathVariable Integer idPista,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return reservaRepository.findByPistaAndFecha(idPista, fecha);
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
     */
    @PostMapping
    @Transactional
    public ResponseEntity<?> crearReserva(@RequestBody Reserva reserva, HttpServletRequest request) {
        Integer idUsuarioToken = (Integer) request.getAttribute("idUsuario");
        if (idUsuarioToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token no válido");
        }

        // Forzamos que la reserva se asigne al usuario que hace la petición
        reserva.setIdUsuario(new Usuario(idUsuarioToken));

        // Validar límite de reservas activas
        if (reservaRepository.countByIdUsuario_IdUsuarioAndEstadoReserva(idUsuarioToken, "activa") >= 2) {
            return error(HttpStatus.FORBIDDEN, "LIMITE_ALCANZADO", "Ya tienes 2 reservas activas.");
        }

        // Validar que el horario no esté ocupado
        List<Reserva> solapadas = reservaRepository.findReservasSolapadas(
                reserva.getIdPista().getIdPista(), reserva.getFecha(), reserva.getHoraInicio(), reserva.getHoraFin()
        );

        if (!solapadas.isEmpty()) {
            return error(HttpStatus.CONFLICT, "HORARIO_OCUPADO", "Este horario ya está reservado");
        }

        // Generar QR y guardar
        reserva.setCodigoQr("RSV-" + reserva.getIdPista().getIdPista() + "-" + idUsuarioToken + "-" + System.currentTimeMillis());
        reserva.setEstadoReserva("activa");
        
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaRepository.save(reserva));
    }

    /**
     * Elimina una reserva (solo el propietario o un admin pueden hacerlo).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarReserva(@PathVariable Integer id, HttpServletRequest request) {
        Reserva reserva = reservaRepository.findById(id).orElse(null);
        if (reserva == null) {
            return error(HttpStatus.NOT_FOUND, "RESERVA_NO_ENCONTRADA", "La reserva no existe");
        }

        // Validar permisos
        if (!reserva.getIdUsuario().getIdUsuario().equals(request.getAttribute("idUsuario")) && !"ADMIN".equals(request.getAttribute("rol"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No puedes cancelar una reserva ajena");
        }

        reservaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Método de ayuda para devolver JSON de errores fácilmente
    private ResponseEntity<?> error(HttpStatus status, String code, String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", code);
        error.put("mensaje", message);
        return ResponseEntity.status(status).body(error);
    }
}
