package com.wishport.frontend.data.dto;

/**
 * DTO para el registro de nuevos usuarios.
 */
public class RegistroRequest {
    private String nombre;
    private String email;
    private String password;
    private String telefono;

    public RegistroRequest(String nombre, String email, String password, String telefono) {
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.telefono = telefono;
    }

    // Getters
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getTelefono() { return telefono; }
}
