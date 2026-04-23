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

    // GET reservas por pista y fecha (para que el frontend bloquee botones)
    @GetMapping("/pista/{idPista}/fecha/{fecha}")
    public List<Reserva> obtenerReservasPorPistaYFecha(
            @PathVariable Integer idPista,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fecha) {
        return reservaRepository.findByPistaAndFecha(idPista, fecha);
    }

    // POST crear reserva (con validación de duplicados)
    @PostMapping
    @Transactional
    public ResponseEntity<?> crearReserva(@RequestBody Reserva reserva) {
        logger.info("=== NUEVA RESERVA ===");
        logger.info("Recibida raw - horaInicio: {}", reserva.getHoraInicio());

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
            error.put("mensaje", "Este horario ya está reservado");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        // Guardar la nueva reserva
        Reserva nuevaReserva = reservaRepository.save(reserva);

        logger.info("Guardada - horaInicio: {}", nuevaReserva.getHoraInicio());

        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaReserva);
    }
}