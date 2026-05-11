package com.wishport.frontend.models;

import java.io.Serializable;

/**
 * MODELO PISTA: Representa una instalación deportiva (Pádel, Tenis, etc.).
 *
 * Esta clase es el "molde" que usa Gson (la librería que procesa JSON) para
 * convertir automáticamente la respuesta del servidor en un objeto Java.
 * Implementa Serializable para poder pasar objetos Pista entre Activities
 * usando Intent.putExtra().
 */
public class Pista implements Serializable {
    /** Identificador único de la pista en la base de datos */
    private int idPista;
    /** Nombre visible de la pista, ej: "Pista Central" */
    private String nombre;
    /** Deporte al que está destinada, ej: "Pádel", "Tenis" */
    private String deporte;
    /** Estado actual de la pista: "disponible" o "mantenimiento" */
    private String estado;
    /** URL de la foto de la pista para mostrarla en el listado */
    private String fotoUrl;

    // --- GETTERS Y SETTERS ---
    // Permiten leer y escribir cada campo de forma controlada.
    // Gson los usa internamente para rellenar el objeto con los datos del JSON.

    /** Devuelve el identificador único de la pista */
    public int getIdPista() { return idPista; }
    /** Establece el identificador único de la pista */
    public void setIdPista(int idPista) { this.idPista = idPista; }

    /** Devuelve el nombre de la pista */
    public String getNombre() { return nombre; }
    /** Establece el nombre de la pista */
    public void setNombre(String nombre) { this.nombre = nombre; }

    /** Devuelve el deporte asociado a la pista */
    public String getDeporte() { return deporte; }
    /** Establece el deporte asociado a la pista */
    public void setDeporte(String deporte) { this.deporte = deporte; }

    /** Devuelve el estado actual de la pista */
    public String getEstado() { return estado; }
    /** Establece el estado actual de la pista */
    public void setEstado(String estado) { this.estado = estado; }

    /** Devuelve la URL de la foto de la pista */
    public String getFotoUrl() { return fotoUrl; }
    /** Establece la URL de la foto de la pista */
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
}
