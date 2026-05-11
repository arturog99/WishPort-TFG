package com.wishport.frontend.data.dto;

/**
 * DTO (Data Transfer Object) para el registro de nuevos usuarios.
 *
 * Contiene exactamente los campos que el backend necesita para crear
 * una cuenta nueva. Evita enviar campos como idUsuario o rol, que
 * no debe decidir el cliente (el servidor asigna el rol USER por defecto).
 */
public class RegistroRequest {
    /** Nombre visible del nuevo usuario */
    private String nombre;
    /** Email único del nuevo usuario, será su identificador de acceso */
    private String email;
    /** Contraseña en texto plano; el backend la cifrará con BCrypt antes de guardarla */
    private String password;
    /** Teléfono de contacto, obligatorio según las reglas del backend */
    private String telefono;

    /**
     * Constructor que recoge todos los datos del formulario de registro.
     * @param nombre   Nombre del usuario.
     * @param email    Email del usuario.
     * @param password Contraseña en texto plano.
     * @param telefono Teléfono del usuario.
     */
    public RegistroRequest(String nombre, String email, String password, String telefono) {
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.telefono = telefono;
    }

    /** Devuelve el nombre para que Gson lo incluya en el JSON */
    public String getNombre() { return nombre; }
    /** Devuelve el email para que Gson lo incluya en el JSON */
    public String getEmail() { return email; }
    /** Devuelve la contraseña para que Gson la incluya en el JSON */
    public String getPassword() { return password; }
    /** Devuelve el teléfono para que Gson lo incluya en el JSON */
    public String getTelefono() { return telefono; }
}
