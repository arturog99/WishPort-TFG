package com.wishport.backend.entities;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.*;

/**
 * Entidad que representa una reserva en el sistema.
 *
 * Mapea directamente con la tabla reservas de la base de datos.
 * Actúa como una tabla intermedia que conecta a un Usuario con
 * una Pista para una fecha y hora específicas.
 *
 */
@Entity
@Table(name = "reservas")
public class Reserva implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Identificador único de la reserva.
     * Generado automáticamente por la base de datos.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva")
    private Integer idReserva;

    /**
     * Fecha en la que se va a jugar.
     * Se usa LocalDate para manejar únicamente el día, mes y año.
     */
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    /**
     * Hora a la que comienza el partido.
     * Se usa LocalTime para manejar únicamente horas y minutos (ej. 09:00).
     */
    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    /**
     * Hora a la que finaliza el partido.
     */
    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    /**
     * Cadena de texto única utilizada para generar el código QR.
     * Se usa para validar el acceso de los jugadores a la instalación deportiva.
     */
    @Column(name = "codigo_qr")
    private String codigoQr;

    /**
     * Estado en el que se encuentra la reserva.
     * Valores habituales: "activa", "completada", "cancelada".
     */
    @Column(name = "estado_reserva")
    private String estadoReserva;

    /**
     * Relación N:1 (Muchos a Uno) con la entidad Pista.
     * Indica la pista deportiva que se está reservando.
     * FetchType.EAGER indica que al consultar una reserva, se traerán siempre
     * los datos de la pista.
     */
    @JoinColumn(name = "id_pista", referencedColumnName = "id_pista")
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Pista idPista;

    /**
     * Relación N:1 (Muchos a Uno) con la entidad Usuario.
     * Indica qué usuario es el titular (creador) de esta reserva.
     * FetchType.EAGER indica que al consultar una reserva, se traerán siempre
     * los datos del usuario titular.
     */
    @JoinColumn(name = "id_usuario", referencedColumnName = "id_usuario")
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Usuario idUsuario;

    // =========================================================================
    // Constructores
    // =========================================================================

    /**
     * Constructor vacío requerido por JPA (Hibernate).
     */
    public Reserva() {}

    /**
     * Constructor para referenciar una reserva únicamente por su ID.
     */
    public Reserva(Integer idReserva) {
        this.idReserva = idReserva;
    }

    /**
     * Constructor básico con los campos de tiempo esenciales.
     */
    public Reserva(Integer idReserva, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        this.idReserva = idReserva;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

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
