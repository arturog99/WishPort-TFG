package com.wishport.frontend.models;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * MODELO RESERVA: Representa el alquiler de una pista por un usuario.
 * Contiene la fecha, las horas de inicio/fin y el código QR generado.
 */
public class Reserva implements Serializable {
    private Integer idReserva;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String codigoQr;      // El texto que se convierte en imagen QR
    private String estadoReserva; // "activa", "cancelada", "finalizada"
    private Pista idPista;        // Objeto Pista completo
    private Usuario idUsuario;    // Objeto Usuario completo

    public Reserva() {}

    // --- GETTERS Y SETTERS ---

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

    public Pista getIdPista() { return idPista; }
    public void setIdPista(Pista idPista) { this.idPista = idPista; }

    public Usuario getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Usuario idUsuario) { this.idUsuario = idUsuario; }
}
