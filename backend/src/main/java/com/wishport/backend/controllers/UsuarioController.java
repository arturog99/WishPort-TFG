package com.wishport.backend.controllers;

import com.wishport.backend.entities.Usuario;
import com.wishport.backend.repositories.UsuarioRepository;
import com.wishport.backend.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST que gestiona todo lo relacionado con los usuarios.
 *
 * Rutas base: /api/usuarios
 *
 * Endpoints:
 *   POST   /api/usuarios              -> registrarUsuario()     [público]
 *   POST   /api/usuarios/login        -> login()                [público]
 *   POST   /api/usuarios/crear-admin  -> crearAdmin()           [privado - requiere JWT y rol ADMIN]
 *   GET    /api/usuarios/me           -> obtenerUsuarioActual() [privado - requiere JWT]
 *   GET    /api/usuarios/{id}         -> obtenerUsuarioPorId()  [privado - requiere JWT]
 *   PUT    /api/usuarios/{id}         -> actualizarUsuario()    [privado - requiere JWT]
 *
 * Las rutas públicas están declaradas en SecurityConfig.filterChain().
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    /** Repositorio para operaciones CRUD sobre la tabla usuarios */
    @Autowired
    private UsuarioRepository usuarioRepository;

    /** Encriptador BCrypt inyectado desde SecurityConfig para comparar/hashear contraseñas */
    @Autowired
    private PasswordEncoder passwordEncoder;

    /** Utilidad para generar y validar tokens JWT */
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Registra un nuevo usuario en el sistema.
     * Ruta: POST /api/usuarios  [pública, no requiere JWT]
     *
     * Flujo:
     * 1. Valida que el teléfono no esté vacío -> 400 BAD REQUEST si falta.
     * 2. Verifica que el email no esté ya registrado -> 409 CONFLICT si duplicado.
     * 3. Hashea la contraseña con BCrypt antes de guardar.
     * 4. Asigna el rol "USER" por defecto (no se puede registrar como ADMIN desde la app).
     * 5. Guarda el usuario y devuelve 200 OK con el objeto guardado.
     *
     * param nuevoUsuario Objeto JSON con nombre, email, password y teléfono.
     * return 200 OK con el usuario, 400/409 con mensaje de error, o 500 si falla la BD.
     */
    @PostMapping
    public ResponseEntity<?> registrarUsuario(@RequestBody Usuario nuevoUsuario) {
        try {
            if (nuevoUsuario.getTelefono() == null || nuevoUsuario.getTelefono().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("El número de teléfono es obligatorio para registrarse");
            }

            if (usuarioRepository.findByEmail(nuevoUsuario.getEmail()) != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Este email ya está registrado");
            }

            // Nunca guardamos la contraseña en texto plano
            nuevoUsuario.setPassword(passwordEncoder.encode(nuevoUsuario.getPassword()));
            nuevoUsuario.setRol("USER"); // Rol por defecto

            return ResponseEntity.ok(usuarioRepository.save(nuevoUsuario));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al registrar: " + e.getMessage());
        }
    }

    /**
     * Registra un nuevo usuario con rol ADMIN.
     * Ruta: POST /api/usuarios/crear-admin  [privada - requiere JWT y rol ADMIN]
     *
     * Flujo:
     * 1. Extrae el rol del usuario autenticado desde el token JWT.
     * 2. Verifica que sea ADMIN -> 403 FORBIDDEN si no.
     * 3. Valida que el teléfono no esté vacío -> 400 BAD REQUEST si falta.
     * 4. Verifica que el email no esté ya registrado -> 409 CONFLICT si duplicado.
     * 5. Hashea la contraseña con BCrypt antes de guardar.
     * 6. Asigna el rol "ADMIN" y guarda el usuario.
     *
     * param request      Petición HTTP con atributo "rol" inyectado por JwtAuthenticationFilter.
     * param nuevoUsuario Objeto JSON con nombre, email, password y teléfono.
     * return 200 OK con el usuario creado, 403 si no es admin, 400/409/500 para otros errores.
     */
    @PostMapping("/crear-admin")
    public ResponseEntity<?> crearAdmin(HttpServletRequest request,
                                        @RequestBody Usuario nuevoUsuario) {
        String rol = (String) request.getAttribute("rol");
        if (!"ADMIN".equals(rol)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Solo los administradores pueden crear otros administradores");
        }

        try {
            if (nuevoUsuario.getTelefono() == null || nuevoUsuario.getTelefono().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("El número de teléfono es obligatorio");
            }

            if (usuarioRepository.findByEmail(nuevoUsuario.getEmail()) != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Este email ya está registrado");
            }

            nuevoUsuario.setPassword(passwordEncoder.encode(nuevoUsuario.getPassword()));
            nuevoUsuario.setRol("ADMIN");

            Usuario guardado = usuarioRepository.save(nuevoUsuario);
            guardado.setPassword(null);
            return ResponseEntity.ok(guardado);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al registrar admin: " + e.getMessage());
        }
    }

    /**
     * Autentica al usuario con email y contraseña.
     * Ruta: POST /api/usuarios/login  [pública, no requiere JWT]
     *
     * Flujo:
     * 1. Busca el usuario por email en la BD.
     * 2. Compara la contraseña recibida con el hash BCrypt guardado.
     * 3. Si coincide: genera un token JWT con id, email y rol.
     * 4. Devuelve 200 OK con el token y los datos del usuario.
     * 5. Si no coincide: devuelve 401 UNAUTHORIZED.
     *
     * La app móvil guarda el token en EncryptedSharedPreferences (TokenManager)
     * y los datos del usuario en SharedPreferences para uso local.
     *
     * param credenciales JSON con email y password.
     * return 200 con {token, idUsuario, nombre, email, telefono, rol} o 401.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Usuario credenciales) {
        Usuario usuario = usuarioRepository.findByEmail(credenciales.getEmail());

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
     * Devuelve los datos del usuario autenticado según el token JWT.
     * Ruta: GET /api/usuarios/me  [privada - requiere JWT]
     *
     * El idUsuario no se pasa por URL sino que se extrae del atributo
     * inyectado en la request por JwtAuthenticationFilter.
     * Esto evita que un usuario pueda consultar datos de otro usuario.
     *
     * param request Petición HTTP con atributo "idUsuario" inyectado por el filtro JWT.
     * return 200 con datos del usuario (sin contraseña), 401 si el token es inválido.
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

        usuario.setPassword(null); // Nunca devolvemos la contraseña (aunque sea hash)
        return ResponseEntity.ok(usuario);
    }

    /**
     * Obtiene los datos de un usuario por su ID.
     * Ruta: GET /api/usuarios/{id}  [privada - requiere JWT]
     *
     * Usado desde PerfilActivity para obtener datos actualizados del servidor,
     * incluyendo el teléfono que no se guarda en SharedPreferences.
     *
     * @param id ID del usuario a consultar.
     * @return 200 con datos del usuario (sin contraseña), 404 si no existe.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerUsuarioPorId(@PathVariable Integer id) {
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
        }

        usuario.setPassword(null); // Nunca devolvemos la contraseña
        return ResponseEntity.ok(usuario);
    }

    /**
     * Actualiza los datos editables del perfil de un usuario.
     * Ruta: PUT /api/usuarios/{id}  [privada - requiere JWT]
     *
     * Solo permite modificar nombre y teléfono. El email, la contraseña
     * y el rol NO se tocan para evitar escaladas de privilegios o
     * cambios accidentales desde el cliente.
     *
     * Flujo:
     * 1. Busca al usuario por ID -> 404 si no existe.
     * 2. Actualiza nombre y teléfono con los valores recibidos.
     * 3. Guarda en BD y devuelve el usuario actualizado (sin contraseña).
     *
     * @param id              ID del usuario a actualizar.
     * @param datosActualizados Objeto con los nuevos valores de nombre y teléfono.
     * @return 200 OK con el usuario actualizado, o 404 si no existe.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarUsuario(@PathVariable Integer id,
                                               @RequestBody Usuario datosActualizados) {
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
        }

        // Solo actualizamos campos editables desde el perfil
        usuario.setNombre(datosActualizados.getNombre());
        usuario.setTelefono(datosActualizados.getTelefono());

        Usuario guardado = usuarioRepository.save(usuario);
        guardado.setPassword(null); // Nunca devolvemos la contraseña
        return ResponseEntity.ok(guardado);
    }
}
