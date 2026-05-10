package com.wishport.backend.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import com.wishport.backend.entities.Reserva;

/**
 * Data Transfer Object (DTO) para la entidad Reserva.
 * Se utiliza para enviar los datos de una reserva al cliente (frontend/móvil)
 * de forma aplanada y segura, sin enviar la entidad completa y evitando
 * posibles bucles infinitos por las relaciones JPA o enviar datos innecesarios.
 */
public class ReservaDTO {
    
    private Integer idReserva;
    
    // Las fechas y horas en el DTO se serializan en formato ISO 8601 automáticamente por Jackson
    private LocalDate fecha;        // Ejemplo: 2026-04-21
    private LocalTime horaInicio;   // Ejemplo: 09:00:00
    private LocalTime horaFin;      // Ejemplo: 10:00:00
    
    private String codigoQr;
    private String estadoReserva;
    
    // Datos aplanados de la Pista
    private Integer idPista;
    private String nombrePista;
    
    // Datos aplanados del Usuario
    private Integer idUsuario;
    private String nombreUsuario;

    /**
     * Constructor vacío requerido por Jackson para la deserialización.
     */
    public ReservaDTO() {}

    /**
     * Constructor que mapea directamente desde la entidad Reserva al DTO.
     * @param reserva La entidad Reserva original extraída de la base de datos.
     */
    public ReservaDTO(Reserva reserva) {
        this.idReserva = reserva.getIdReserva();
        this.codigoQr = reserva.getCodigoQr();
        this.estadoReserva = reserva.getEstadoReserva();

        // Extraer solo la información necesaria de la pista
        if (reserva.getIdPista() != null) {
            this.idPista = reserva.getIdPista().getIdPista();
            this.nombrePista = reserva.getIdPista().getNombre();
        }

        // Extraer solo la información necesaria del usuario
        if (reserva.getIdUsuario() != null) {
            this.idUsuario = reserva.getIdUsuario().getIdUsuario();
            this.nombreUsuario = reserva.getIdUsuario().getNombre();
        }

        this.fecha = reserva.getFecha();
        this.horaInicio = reserva.getHoraInicio();
        this.horaFin = reserva.getHoraFin();
    }

    // --- Getters y Setters ---

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
