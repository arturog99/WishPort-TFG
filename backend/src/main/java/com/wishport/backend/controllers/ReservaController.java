
package com.wishport.backend.controllers;


import com.wishport.backend.entities.Reserva;

import com.wishport.backend.repositories.ReservaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private static final Logger logger = LoggerFactory.getLogger(ReservaController.class);

    @Autowired
    private ReservaRepository reservaRepository;

    // GET todas las reservas (existente)
    @GetMapping
    public List<Reserva> obtenerTodasLasReservas() {
        return reservaRepository.findAll();
    }

    // GET reservas por usuario
    @GetMapping("/usuario/{idUsuario}")
    public List<Reserva> obtenerReservasPorUsuario(@PathVariable Integer idUsuario) {
        return reservaRepository.findByIdUsuario_IdUsuario(idUsuario);
    }

    // GET reservas por pista y fecha (para que el frontend bloquee botones)
    @GetMapping("/pista/{idPista}/fecha/{fecha}")
    public List<Reserva> obtenerReservasPorPistaYFecha(
            @PathVariable Integer idPista,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fecha) {
        return reservaRepository.findByPistaAndFecha(idPista, fecha);
    }

    // POST crear reserva (con validacion de duplicados)
    @PostMapping
    @Transactional
    public ResponseEntity<?> crearReserva(@RequestBody Reserva reserva) {
        logger.info("=== NUEVA RESERVA ===");

        // Suponemos que el limite son 2 reservas "activas"
        long activas = reservaRepository.countByIdUsuario_IdUsuarioAndEstadoReserva(
                reserva.getIdUsuario().getIdUsuario(),
                "activa"
        );

        if (activas >= 2) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "LIMITE_ALCANZADO");
            error.put("mensaje", "Ya tienes 2 reservas activas. Juega tus partidos para poder reservar mÃƒÂ¡s.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        // Validar que no exista una reserva solapada
        List<Reserva> reservasExistentes = reservaRepository.findReservasSolapadas(
                reserva.getIdPista().getIdPista(),
                reserva.getFecha(),
                reserva.getHoraInicio(),
                reserva.getHoraFin()
        );

        if (!reservasExistentes.isEmpty()) {
            // Ya existe una reserva para este horario - devolver 409 CONFLICT
            Map<String, String> error = new HashMap<>();
            error.put("error", "HORARIO_OCUPADO");
            error.put("mensaje", "Este horario ya estÃƒÂ¡ reservado");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        // Generar código QR único (ej: RESERVA-123-1714483923)
        String codigoQR = "RESERVA-" + reserva.getIdPista().getIdPista() + "-" +
                reserva.getIdUsuario().getIdUsuario() + "-" +
                System.currentTimeMillis();
        reserva.setCodigoQr(codigoQR);

        // Guardar la nueva reserva
        reserva.setEstadoReserva("activa");
        Reserva nuevaReserva = reservaRepository.save(reserva);

        // Devolvemos la entidad original
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaReserva);   }

    // DELETE eliminar reserva por ID
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarReserva(@PathVariable Integer id) {
        if (!reservaRepository.existsById(id)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "RESERVA_NO_ENCONTRADA");
            error.put("mensaje", "La reserva no existe");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        reservaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

