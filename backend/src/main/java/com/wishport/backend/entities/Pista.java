/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.wishport.backend.entities;


import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
/**
 *
 * @author Arturo
 */
@Entity
@Table(name = "pistas")

public class Pista implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id_pista")
    private Integer idPista;
    @Basic(optional = false)
    @Column(name = "nombre")
    private String nombre;
    @Basic(optional = false)
    @Column(name = "deporte")
    private String deporte;
    @Column(name = "foto_url")
    private String fotoUrl;
    @Column(name = "estado")
    private String estado;
    @JsonIgnore
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "idPista", fetch = FetchType.EAGER)
    private List<Reserva> reservasList;

    public Pista() {
    }

    public Pista(Integer idPista) {
        this.idPista = idPista;
    }

    public Pista(Integer idPista, String nombre, String deporte) {
        this.idPista = idPista;
        this.nombre = nombre;
        this.deporte = deporte;
    }

    public Integer getIdPista() {
        return idPista;
    }

    public void setIdPista(Integer idPista) {
        this.idPista = idPista;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDeporte() {
        return deporte;
    }

    public void setDeporte(String deporte) {
        this.deporte = deporte;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public List<Reserva> getReservasList() {
        return reservasList;
    }

    public void setReservasList(List<Reserva> reservasList) {
        this.reservasList = reservasList;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idPista != null ? idPista.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Pista)) {
            return false;
        }
        Pista other = (Pista) object;
        if ((this.idPista == null && other.idPista != null) || (this.idPista != null && !this.idPista.equals(other.idPista))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.arturo.telegrambot.Pistas[ idPista=" + idPista + " ]";
    }
    
}
