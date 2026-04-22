package com.wishport.frontend.models;

/**
 * Clase que representa el modelo de un Usuario (entidad del Backend).
 */
public class Usuario {
    private Integer idUsuario;
    private String nombre;
    private String email;
    private String password;
    private String rol;

    public Usuario() {
    }

    public Usuario(Integer idUsuario, String nombre, String email, String password) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.email = email;
        this.password = password;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}
