package com.wishport.backend.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.wishport.backend.entities.Reserva;

public class ReservaDTO {
    private Integer idReserva;
    private LocalDate fecha;        // ISO 8601: 2026-04-21
    private LocalTime horaInicio;   // ISO 8601: 09:00:00
    private LocalTime horaFin;      // ISO 8601: 10:00:00
    private String codigoQr;
    private String estadoReserva;
    private Integer idPista;
    private String nombrePista;
    private Integer idUsuario;
    private String nombreUsuario;

    private static final ZoneId EUROPE_MADRID = ZoneId.of("Europe/Madrid");

    public ReservaDTO() {}

    public ReservaDTO(Reserva reserva) {
        this.idReserva = reserva.getIdReserva();
        this.codigoQr = reserva.getCodigoQr();
        this.estadoReserva = reserva.getEstadoReserva();

        if (reserva.getIdPista() != null) {
            this.idPista = reserva.getIdPista().getIdPista();
            this.nombrePista = reserva.getIdPista().getNombre();
        }

        if (reserva.getIdUsuario() != null) {
            this.idUsuario = reserva.getIdUsuario().getIdUsuario();
            this.nombreUsuario = reserva.getIdUsuario().getNombre();
        }

        // java.time: usar directamente LocalDate y LocalTime
        this.fecha = reserva.getFecha();
        this.horaInicio = reserva.getHoraInicio();
        this.horaFin = reserva.getHoraFin();
    }

    // Getters y Setters
    public Integer getIdReserva() { return idReserva; }
    public void setIdReserva(Integer idReserva) { this.idReserva = idReserva; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }

    public String getCodigoQr() { return codigoQr; }
    public void setCodigoQr(String codigoQr) { this.codigoQr = codigoQr; }

    public String getEstadoReserva() { return estadoReserva; }
    public void setEstadoReserva(String estadoReserva) { this.estadoReserva = estadoReserva; }

    public Integer getIdPista() { return idPista; }
    public void setIdPista(Integer idPista) { this.idPista = idPista; }

    public String getNombrePista() { return nombrePista; }
    public void setNombrePista(String nombrePista) { this.nombrePista = nombrePista; }

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
}
