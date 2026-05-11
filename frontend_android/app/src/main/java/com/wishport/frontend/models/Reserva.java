package com.wishport.frontend.models;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * MODELO RESERVA: Representa el alquiler de una pista por un usuario.
 * Contiene la fecha, las horas de inicio/fin y el código QR generado.
 *
 * Es la entidad central de la aplicación: une a un Usuario con una Pista
 * en una franja horaria concreta. Implementa Serializable para poder
 * pasarse entre Activities con Intent.putExtra().
 */
public class Reserva implements Serializable {
    /** Identificador único de la reserva generado por la base de datos */
    private Integer idReserva;
    /** Fecha del día de la reserva (solo año/mes/día, sin hora) */
    private LocalDate fecha;
    /** Hora a la que comienza la franja reservada, ej: 18:00 */
    private LocalTime horaInicio;
    /** Hora a la que termina la franja reservada, ej: 19:00 */
    private LocalTime horaFin;
    /** Código único en texto que el frontend convierte en imagen QR */
    private String codigoQr;
    /** Estado actual: "activa", "cancelada" o "completada" */
    private String estadoReserva;
    /** Objeto Pista completo con todos sus datos (nombre, deporte, etc.) */
    private Pista idPista;
    /** Objeto Usuario completo con todos sus datos (nombre, email, etc.) */
    private Usuario idUsuario;

    /** Constructor vacío requerido por Gson para crear instancias al parsear JSON */
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
