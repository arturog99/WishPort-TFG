/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.wishport.backend.entities;

import java.io.Serial;
import java.io.Serializable;

import java.util.Date;

import jakarta.persistence.*;

/**
 *
 * @author Arturo
 */
@Entity
@Table(name = "reservas")

public class Reserva implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id_reserva")
    private Integer idReserva;
    @Basic(optional = false)
    @Column(name = "fecha")
    @Temporal(TemporalType.DATE)
    private Date fecha;
    @Basic(optional = false)
    @Column(name = "hora_inicio")
    @Temporal(TemporalType.TIME)
    private Date horaInicio;
    @Basic(optional = false)
    @Column(name = "hora_fin")
    @Temporal(TemporalType.TIME)
    private Date horaFin;
    @Column(name = "codigo_qr")
    private String codigoQr;
    @Column(name = "estado_reserva")
    private String estadoReserva;
    @JoinColumn(name = "id_pista", referencedColumnName = "id_pista")
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Pista idPista;
    @JoinColumn(name = "id_usuario", referencedColumnName = "id_usuario")
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Usuario idUsuario;

    public Reserva() {
    }

    public Reserva(Integer idReserva) {
        this.idReserva = idReserva;
    }

    public Reserva(Integer idReserva, Date fecha, Date horaInicio, Date horaFin) {
        this.idReserva = idReserva;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    public Integer getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(Integer idReserva) {
        this.idReserva = idReserva;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public Date getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(Date horaInicio) {
        this.horaInicio = horaInicio;
    }

    public Date getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(Date horaFin) {
        this.horaFin = horaFin;
    }

    public String getCodigoQr() {
        return codigoQr;
    }

    public void setCodigoQr(String codigoQr) {
        this.codigoQr = codigoQr;
    }

    public String getEstadoReserva() {
        return estadoReserva;
    }

    public void setEstadoReserva(String estadoReserva) {
        this.estadoReserva = estadoReserva;
    }

    public Pista getIdPista() {
        return idPista;
    }

    public void setIdPista(Pista idPista) {
        this.idPista = idPista;
    }

    public Usuario getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Usuario idUsuario) {
        this.idUsuario = idUsuario;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idReserva != null ? idReserva.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Reserva)) {
            return false;
        }
        Reserva other = (Reserva) object;
        if ((this.idReserva == null && other.idReserva != null) || (this.idReserva != null && !this.idReserva.equals(other.idReserva))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.arturo.telegrambot.Reservas[ idReserva=" + idReserva + " ]";
    }
    
}
