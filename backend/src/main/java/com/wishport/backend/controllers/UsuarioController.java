package com.wishport.backend.controllers;

import com.wishport.backend.entities.Usuario;

import com.wishport.backend.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController

//@RequestMapping. Define la ruta principal de la URL. Indica que todos los métodos que haya dentro de esta clase
//van a colgar de la ruta http://localhost:8080/api/usuarios
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    //Variable que actuará como tu puente hacia la tabla de usuarios en MySQL.
    //Gracias a esto no es necesario que escribir sentencias SQL a mano.
    private UsuarioRepository usuarioRepository;

    @Autowired
    //Encoder para encriptar y verificar contraseñas con BCrypt
    private PasswordEncoder passwordEncoder;

    //@GetMapping.Indica que el método de abajo se activará solo cuando alguien haga una
    //petición HTTP de tipo GET a /api/usuarios
    @GetMapping
    public List<Usuario> obtenerTodosLosUsuarios() {
        return usuarioRepository.findAll();
    }

    //@PostMapping.Indica que este método reaccionará cuando alguien haga una petición HTTP
    //de tipo POST
    @PostMapping
    //@RequestBody. Esta etiqueta coge el paquete de texto JSON que envía Antonio desde la app de Android
    //y lo convierte a un objeto Usuario
    public ResponseEntity<?> registrarUsuario(@RequestBody Usuario nuevoUsuario) {
        try {
            // Verificar si el email ya existe
            Usuario existente = usuarioRepository.findByEmail(nuevoUsuario.getEmail());
            if (existente != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Este email ya está registrado");
            }

            // Recibe un JSON desde el móvil, lo convierte en un objeto Usuario
            // Encriptamos la contraseña antes de guardarla en la base de datos
            String hashedPassword = passwordEncoder.encode(nuevoUsuario.getPassword());
            nuevoUsuario.setPassword(hashedPassword);
            // y lo guarda automáticamente en la tabla 'usuarios' de MySQL.
            Usuario guardado = usuarioRepository.save(nuevoUsuario);
            return ResponseEntity.ok(guardado);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al registrar: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Usuario> login(@RequestBody Usuario credenciales) {
        // 1. Buscamos en la BBDD si existe alguien con ese email
        Usuario usuario = usuarioRepository.findByEmail(credenciales.getEmail());

        // 2. Si existe Y ADEMÁS la contraseña coincide...
        if (usuario != null && passwordEncoder.matches(credenciales.getPassword(), usuario.getPassword())) {
            // ¡Éxito! Devolvemos un código 200 (OK) y los datos del usuario
            return ResponseEntity.ok(usuario);
        }

        // 3. Si el email no existe o la contraseña está mal...
        // Devolvemos un código 401 (Unauthorized) sin datos
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}