package com.wishport.frontend.data.dto;

/**
 * DTO (Data Transfer Object) para el inicio de sesión.
 *
 * En lugar de enviar el objeto Usuario completo (con campos como id, rol, etc.),
 * este DTO garantiza que al servidor solo llegan los dos campos estrictamente
 * necesarios para hacer login: email y contraseña.
 * Esto mejora la seguridad y evita enviar datos innecesarios al backend.
 */
public class LoginRequest {
    /** Correo electrónico del usuario que intenta iniciar sesión */
    private String email;
    /** Contraseña en texto plano (el backend la compara con el hash almacenado) */
    private String password;

    /**
     * Constructor que crea el DTO con los datos del formulario de login.
     * @param email   Email introducido por el usuario.
     * @param password Contraseña introducida por el usuario.
     */
    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    /** Devuelve el email para que Gson lo serialice en el JSON */
    public String getEmail() { return email; }
    /** Devuelve la contraseña para que Gson la serialice en el JSON */
    public String getPassword() { return password; }
}
