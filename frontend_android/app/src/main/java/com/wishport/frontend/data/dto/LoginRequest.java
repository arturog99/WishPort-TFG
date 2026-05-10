package com.wishport.frontend.data.dto;

/**
 * DTO para el inicio de sesión.
 * Solo contiene los campos necesarios para validar al usuario.
 */
public class LoginRequest {
    private String email;
    private String password;

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() { return email; }
    public String getPassword() { return password; }
}
