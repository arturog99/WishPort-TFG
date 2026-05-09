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

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping
    public ResponseEntity<?> registrarUsuario(@RequestBody Usuario nuevoUsuario) {
        try {
            Usuario existente = usuarioRepository.findByEmail(nuevoUsuario.getEmail());
            if (existente != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Este email ya está registrado");
            }

            String hashedPassword = passwordEncoder.encode(nuevoUsuario.getPassword());
            nuevoUsuario.setPassword(hashedPassword);
            nuevoUsuario.setRol("USER");
            Usuario guardado = usuarioRepository.save(nuevoUsuario);
            return ResponseEntity.ok(guardado);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al registrar: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Usuario credenciales) {
        Usuario usuario = usuarioRepository.findByEmail(credenciales.getEmail());

        if (usuario != null && passwordEncoder.matches(credenciales.getPassword(), usuario.getPassword())) {
            String token = jwtUtil.generarToken(
                    usuario.getIdUsuario(),
                    usuario.getEmail(),
                    usuario.getRol()
            );

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("token", token);
            respuesta.put("idUsuario", usuario.getIdUsuario());
            respuesta.put("nombre", usuario.getNombre());
            respuesta.put("email", usuario.getEmail());
            respuesta.put("rol", usuario.getRol());

            return ResponseEntity.ok(respuesta);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

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

        // No devolver la contraseña
        usuario.setPassword(null);
        return ResponseEntity.ok(usuario);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerUsuarioPorId(@PathVariable Integer id) {
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
        }

        // No devolver la contraseña
        usuario.setPassword(null);
        return ResponseEntity.ok(usuario);
    }
}
