package com.wishport.frontend.models;

import java.io.Serializable;

/**
 * MODELO USUARIO: Representa a una persona en el sistema.
 * Se usa tanto para el Login como para mostrar datos en el Perfil.
 *
 * Implementa Serializable para poder pasarse entre Activities.
 * Gson la usa para convertir el JSON del servidor en este objeto Java.
 */
public class Usuario implements Serializable {
    /** Identificador único del usuario en la base de datos */
    private Integer idUsuario;
    /** Nombre completo o de perfil del usuario */
    private String nombre;
    /** Correo electrónico, usado como nombre de usuario para el login */
    private String email;
    /** Contraseña. Solo se envía en login/registro, el servidor NUNCA la devuelve */
    private String password;
    /** Rol del usuario: "USER" (usuario normal) o "ADMIN" (administrador) */
    private String rol;
    /** Teléfono de contacto del usuario */
    private String telefono;

    /** Constructor vacío requerido por Gson */
    public Usuario() {}

    /**
     * Constructor completo para crear un usuario con todos sus datos.
     * Usado cuando se necesita construir manualmente un objeto Usuario.
     */
    public Usuario(Integer idUsuario, String nombre, String email, String password, String telefono) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.telefono = telefono;
    }

    // --- GETTERS Y SETTERS (Acceso a los datos) ---

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
}
