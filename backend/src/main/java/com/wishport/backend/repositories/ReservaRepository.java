package com.wishport.backend.repositories;

import com.wishport.backend.entities.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;



/**
 * Repositorio JPA para la entidad Reserva.
 *
 * Hereda los métodos CRUD estándar de JpaRepository y añade
 * consultas personalizadas (JPQL y naming conventions) para las
 * operaciones específicas del sistema de reservas.
 */
@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    /**
     * Obtiene todas las reservas de una pista concreta para una fecha dada.
     * Se usa en DetallePistaActivity para colorear los botones de hora
     * (cuales están ocupados y cuáles libres).
     *
     * Ruta que lo usa: GET /api/reservas/pista/{idPista}/fecha/{fecha}
     *
     * @param idPista ID de la pista a consultar.
     * @param fecha   Fecha para la que se quieren saber las reservas.
     * @return Lista de reservas activas o pasadas para esa pista y fecha.
     */
    @Query("SELECT r FROM Reserva r WHERE r.idPista.idPista = :idPista AND r.fecha = :fecha AND r.estadoReserva = 'activa'")
    List<Reserva> findByPistaAndFecha(@Param("idPista") Integer idPista, @Param("fecha") LocalDate fecha);

    /**
     * Detecta si existe alguna reserva que se solape con el horario solicitado.
     * La condición de solapamiento es: horaInicio_existente < horaFin_nueva AND horaFin_existente > horaInicio_nueva.
     * Esto cubre todos los casos posibles de solapamiento parcial o total.
     *
     * Se usa en dos sitios:
     *   1. GET /api/reservas/disponibilidad  -> verificación previa antes del checkout.
     *   2. POST /api/reservas                -> verificación final al crear la reserva.
     *
     * @param idPista    ID de la pista.
     * @param fecha      Fecha de la reserva.
     * @param horaInicio Hora de inicio del hueco solicitado.
     * @param horaFin    Hora de fin del hueco solicitado.
     * @return Lista de reservas solapadas (vacía = disponible).
     */
    @Query("SELECT r FROM Reserva r WHERE r.idPista.idPista = :idPista " +
            "AND r.fecha = :fecha " +
            "AND r.estadoReserva = 'activa' " +
            "AND ((r.horaInicio < :horaFin AND r.horaFin > :horaInicio))")
    List<Reserva> findReservasSolapadas(@Param("idPista") Integer idPista,
                                        @Param("fecha") LocalDate fecha,
                                        @Param("horaInicio") LocalTime horaInicio,
                                        @Param("horaFin") LocalTime horaFin);

    /**
     * Cuenta cuántas reservas "activas" tiene un usuario.
     * Spring Data JPA genera el SQL desde el nombre del método:
     *   SELECT COUNT(*) FROM reservas WHERE id_usuario = ? AND estado_reserva = ?
     *
     * Se usa para limitar a 2 reservas activas simultáneas por usuario.
     *
     * @param idUsuario     ID del usuario.
     * @param estadoReserva Estado a filtrar (ej: "activa").
     * @return Número de reservas con ese estado para ese usuario.
     */
    long countByIdUsuario_IdUsuarioAndEstadoReserva(Integer idUsuario, String estadoReserva);

    /**
     * Obtiene todas las reservas de un usuario concreto.
     * Spring Data JPA genera: SELECT * FROM reservas WHERE id_usuario = ?
     *
     * Se usa en GET /api/reservas/usuario/{idUsuario} para mostrar
     * el historial en ReservasActivity.
     *
     * @param idUsuario ID del usuario.
     * @return Lista de todas las reservas del usuario (activas, completadas y canceladas).
     */
    List<Reserva> findByIdUsuario_IdUsuario(Integer idUsuario);

    /**
     * Obtiene todas las reservas de una fecha concreta.
     * Spring Data JPA genera: SELECT * FROM reservas WHERE fecha = ?
     *
     * Se usa en GET /api/reservas/hoy para que el administrador
     * vea las reservas del día en AdminActivity.
     *
     * @param fecha Fecha a consultar.
     * @return Lista de reservas para esa fecha.
     */
    List<Reserva> findByFecha(LocalDate fecha);
}
