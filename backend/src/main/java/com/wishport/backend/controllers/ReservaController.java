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

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    @Autowired
    private ReservaRepository reservaRepository;

    // GET todas las reservas (solo ADMIN)
    @GetMapping
    public List<Reserva> obtenerTodasLasReservas(HttpServletRequest request) {
        String rol = (String) request.getAttribute("rol");
        if (!"ADMIN".equals(rol)) {
            throw new org.springframework.security.access.AccessDeniedException("Acceso denegado");
        }
        return reservaRepository.findAll();
    }

    // GET reservas del día de hoy (para panel de administración - solo ADMIN)
    @GetMapping("/hoy")
    public List<Reserva> obtenerReservasDeHoy(HttpServletRequest request) {
        String rol = (String) request.getAttribute("rol");
        if (!"ADMIN".equals(rol)) {
            throw new org.springframework.security.access.AccessDeniedException("Acceso denegado");
        }
        LocalDate hoy = LocalDate.now();
        return reservaRepository.findByFecha(hoy);
    }

    // GET reservas del usuario autenticado
    @GetMapping("/usuario/{idUsuario}")
    @Transactional
    public List<Reserva> obtenerReservasPorUsuario(@PathVariable Integer idUsuario,
                                                   HttpServletRequest request) {
        Integer idUsuarioToken = (Integer) request.getAttribute("idUsuario");
        String rol = (String) request.getAttribute("rol");

        // Solo puede ver sus propias reservas o si es ADMIN
        if (!idUsuario.equals(idUsuarioToken) && !"ADMIN".equals(rol)) {
            throw new org.springframework.security.access.AccessDeniedException("Acceso denegado");
        }

        List<Reserva> reservas = reservaRepository.findByIdUsuario_IdUsuario(idUsuario);
        ZonedDateTime ahora = ZonedDateTime.now(ZoneId.of("Europe/Madrid"));
        boolean hayActualizaciones = false;

        for (Reserva reserva : reservas) {
            if ("activa".equals(reserva.getEstadoReserva())) {
                ZonedDateTime finReserva = ZonedDateTime.of(
                    reserva.getFecha(),
                    reserva.getHoraFin(),
                    ZoneId.of("Europe/Madrid")
                );
                if (finReserva.isBefore(ahora)) {
                    reserva.setEstadoReserva("completada");
                    hayActualizaciones = true;
                }
            }
        }

        if (hayActualizaciones) {
            reservaRepository.saveAll(reservas);
        }

        return reservas;
    }

    // GET reservas por pista y fecha (público para consultar disponibilidad)
    @GetMapping("/pista/{idPista}/fecha/{fecha}")
    public List<Reserva> obtenerReservasPorPistaYFecha(
            @PathVariable Integer idPista,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return reservaRepository.findByPistaAndFecha(idPista, fecha);
    }

    // POST crear reserva (usuario autenticado)
    @PostMapping
    @Transactional
    public ResponseEntity<?> crearReserva(@RequestBody Reserva reserva, HttpServletRequest request) {
        Integer idUsuarioToken = (Integer) request.getAttribute("idUsuario");
        if (idUsuarioToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token no válido");
        }

        // Sustituir el usuario del body por el autenticado (evita spoofing)
        Usuario usuarioAutenticado = new Usuario(idUsuarioToken);
        reserva.setIdUsuario(usuarioAutenticado);

        long activas = reservaRepository.countByIdUsuario_IdUsuarioAndEstadoReserva(
                idUsuarioToken, "activa"
        );

        if (activas >= 2) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "LIMITE_ALCANZADO");
            error.put("mensaje", "Ya tienes 2 reservas activas. Juega tus partidos para poder reservar más.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        List<Reserva> reservasExistentes = reservaRepository.findReservasSolapadas(
                reserva.getIdPista().getIdPista(),
                reserva.getFecha(),
                reserva.getHoraInicio(),
                reserva.getHoraFin()
        );

        if (!reservasExistentes.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "HORARIO_OCUPADO");
            error.put("mensaje", "Este horario ya está reservado");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        String codigoQR = "RESERVA-" + reserva.getIdPista().getIdPista() + "-" +
                idUsuarioToken + "-" +
                System.currentTimeMillis();
        reserva.setCodigoQr(codigoQR);
        reserva.setEstadoReserva("activa");
        Reserva nuevaReserva = reservaRepository.save(reserva);

        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaReserva);
    }

    // DELETE eliminar reserva por ID (solo propietario o ADMIN)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarReserva(@PathVariable Integer id, HttpServletRequest request) {
        Integer idUsuarioToken = (Integer) request.getAttribute("idUsuario");
        String rol = (String) request.getAttribute("rol");

        Reserva reserva = reservaRepository.findById(id).orElse(null);
        if (reserva == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "RESERVA_NO_ENCONTRADA");
            error.put("mensaje", "La reserva no existe");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        // Verificar propiedad
        if (!idUsuarioToken.equals(reserva.getIdUsuario().getIdUsuario()) && !"ADMIN".equals(rol)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No puedes cancelar una reserva ajena");
        }

        reservaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

