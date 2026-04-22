package com.wishport.backend.repositories;

import com.wishport.backend.entities.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    // Busca reservas de una pista específica para una fecha específica
    @Query("SELECT r FROM Reserva r WHERE r.idPista.idPista = :idPista AND r.fecha = :fecha")
    List<Reserva> findByPistaAndFecha(@Param("idPista") Integer idPista, @Param("fecha") Date fecha);

    // Busca reservas que se solapen con el horario solicitado (para validar duplicados)
    @Query("SELECT r FROM Reserva r WHERE r.idPista.idPista = :idPista " +
            "AND r.fecha = :fecha " +
            "AND ((r.horaInicio < :horaFin AND r.horaFin > :horaInicio))")
    List<Reserva> findReservasSolapadas(@Param("idPista") Integer idPista,
                                        @Param("fecha") Date fecha,
                                        @Param("horaInicio") Date horaInicio,
                                        @Param("horaFin") Date horaFin);
}