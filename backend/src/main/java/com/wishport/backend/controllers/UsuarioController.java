package com.wishport.backend.controllers;

import com.wishport.backend.entities.Usuario;
import com.wishport.backend.repositories.UsuarioRepository;
import com.wishport.backend.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para gestionar las operaciones relacionadas con los usuarios.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Registra un nuevo usuario en el sistema.
     * @param nuevoUsuario Datos del usuario a registrar.
     * @return El usuario guardado o un mensaje de error si faltan datos o el email ya existe.
     */
    @PostMapping
    public ResponseEntity<?> registrarUsuario(@RequestBody Usuario nuevoUsuario) {
        try {
            // Validar que el teléfono esté presente
            if (nuevoUsuario.getTelefono() == null || nuevoUsuario.getTelefono().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("El número de teléfono es obligatorio para registrarse");
            }

            // Verificar si el email ya está en uso
            if (usuarioRepository.findByEmail(nuevoUsuario.getEmail()) != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Este email ya está registrado");
            }

            // Encriptar la contraseña y asignar rol por defecto
            nuevoUsuario.setPassword(passwordEncoder.encode(nuevoUsuario.getPassword()));
            nuevoUsuario.setRol("USER");
            
            return ResponseEntity.ok(usuarioRepository.save(nuevoUsuario));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al registrar: " + e.getMessage());
        }
    }

    /**
     * Inicia sesión verificando las credenciales.
     * @param credenciales Objeto con email y password.
     * @return Un token JWT y los datos del usuario, o estado 401 si falla.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Usuario credenciales) {
        Usuario usuario = usuarioRepository.findByEmail(credenciales.getEmail());

        // Comprobar que el usuario existe y la contraseña coincide
        if (usuario != null && passwordEncoder.matches(credenciales.getPassword(), usuario.getPassword())) {
            String token = jwtUtil.generarToken(usuario.getIdUsuario(), usuario.getEmail(), usuario.getRol());

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("token", token);
            respuesta.put("idUsuario", usuario.getIdUsuario());
            respuesta.put("nombre", usuario.getNombre());
            respuesta.put("email", usuario.getEmail());
            respuesta.put("telefono", usuario.getTelefono());
            respuesta.put("rol", usuario.getRol());

            return ResponseEntity.ok(respuesta);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    /**
     * Devuelve los datos del usuario autenticado actualmente según su token.
     * @param request Petición HTTP con el token interceptado.
     * @return Datos del usuario.
     */
    @GetMapping("/me")
    public ResponseEntity<?> obtenerUsuarioActual(HttpServletRequest request) {
        Integer idUsuario = (Integer) request.getAttribute("idUsuario");
        if (idUsuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token no válido");
        }

        Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
        }

        usuario.setPassword(null); // Ocultar contraseña por seguridad
        return ResponseEntity.ok(usuario);
    }

    /**
     * Obtiene un usuario por su ID.
     * @param id Identificador del usuario.
     * @return Datos del usuario.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerUsuarioPorId(@PathVariable Integer id) {
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
        }

        usuario.setPassword(null); // Ocultar contraseña por seguridad
        return ResponseEntity.ok(usuario);
    }
}
