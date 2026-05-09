package com.wishport.frontend.models;

import java.io.Serializable;

/**
 * MODELO PISTA: Representa una instalación deportiva (Pádel, Tenis, etc.).
 */
public class Pista implements Serializable {
    private int idPista;
    private String nombre;
    private String deporte;
    private String estado; // Ejemplo: "Disponible", "Mantenimiento"
    private String fotoUrl;

    // --- GETTERS Y SETTERS ---

    public int getIdPista() { return idPista; }
    public void setIdPista(int idPista) { this.idPista = idPista; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDeporte() { return deporte; }
    public void setDeporte(String deporte) { this.deporte = deporte; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
}
