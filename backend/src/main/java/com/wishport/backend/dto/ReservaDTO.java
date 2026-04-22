package com.wishport.backend.dto;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import com.wishport.backend.entities.Reserva;

public class ReservaDTO {
    private Integer idReserva;
    private String fecha;  // ISO 8601: 2026-04-21
    private String horaInicio;  // ISO 8601 con zona: 2026-04-21T08:00:00Z
    private String horaFin;     // ISO 8601 con zona: 2026-04-21T09:00:00Z
    private String codigoQr;
    private String estadoReserva;
    private Integer idPista;
    private String nombrePista;
    private Integer idUsuario;
    private String nombreUsuario;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
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

        // En ReservaDTO.java constructor
        if (reserva.getFecha() != null && reserva.getHoraInicio() != null && reserva.getHoraFin() != null) {
            // Combinar fecha y hora para crear timestamps completos en zona española
            ZonedDateTime fechaInicio = combinarFechaHora(reserva.getFecha(), reserva.getHoraInicio());
            ZonedDateTime fechaFin = combinarFechaHora(reserva.getFecha(), reserva.getHoraFin());

            // Formatear en ISO 8601 con offset de España (ej: 2026-04-22T08:00:00+02:00)
            this.fecha = fechaInicio.format(DateTimeFormatter.ISO_LOCAL_DATE);
            this.horaInicio = fechaInicio.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            this.horaFin = fechaFin.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
    }

    private ZonedDateTime combinarFechaHora(Date fecha, Date hora) {
        // Convertir a ZonedDateTime en zona horaria de España
        ZonedDateTime fechaZDT = ZonedDateTime.ofInstant(fecha.toInstant(), EUROPE_MADRID);
        ZonedDateTime horaZDT = ZonedDateTime.ofInstant(hora.toInstant(), EUROPE_MADRID);
        
        // Combinar: fecha + hora
        return fechaZDT.withHour(horaZDT.getHour())
                      .withMinute(horaZDT.getMinute())
                      .withSecond(horaZDT.getSecond());
    }

    // Getters y Setters
    public Integer getIdReserva() { return idReserva; }
    public void setIdReserva(Integer idReserva) { this.idReserva = idReserva; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getHoraInicio() { return horaInicio; }
    public void setHoraInicio(String horaInicio) { this.horaInicio = horaInicio; }

    public String getHoraFin() { return horaFin; }
    public void setHoraFin(String horaFin) { this.horaFin = horaFin; }

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
