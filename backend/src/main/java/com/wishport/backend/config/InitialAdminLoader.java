package com.wishport.backend.config;

import com.wishport.backend.entities.Usuario;
import com.wishport.backend.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Componente de inicialización que crea automáticamente el primer administrador
 * si no existe ningún usuario con rol ADMIN en la base de datos.
 *
 * Requiere la variable de entorno WISHPORT_ADMIN_INITIAL_PASSWORD definida.
 * La aplicación NO arrancará si no hay admins en la BD y esta variable no está definida
 * o si la password tiene menos de 8 caracteres.
 *
 * Se ejecuta automáticamente al arrancar la aplicación (CommandLineRunner).
 */
@Component
public class InitialAdminLoader implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${WISHPORT_ADMIN_INITIAL_PASSWORD:}")
    private String adminInitialPassword;

    @Override
    public void run(String... args) {
        // Verificar si ya existe algún admin en la base de datos
        List<Usuario> usuarios = usuarioRepository.findAll();
        boolean existeAdmin = usuarios.stream()
                .anyMatch(u -> "ADMIN".equals(u.getRol()));

        if (existeAdmin) {
            System.out.println("✅ Ya existe al menos un administrador en el sistema.");
            return;
        }

        // No hay admins, verificar que la variable de entorno está definida
        if (adminInitialPassword == null || adminInitialPassword.trim().isEmpty()) {
            System.err.println("═══════════════════════════════════════════════════════════════");
            System.err.println("❌ ERROR CRÍTICO: No se encontró ningún administrador en la BD");
            System.err.println("   y no está definida la variable de entorno:");
            System.err.println("   WISHPORT_ADMIN_INITIAL_PASSWORD");
            System.err.println("");
            System.err.println("   Debes definir esta variable con una contraseña segura");
            System.err.println("   (mínimo 8 caracteres) antes de iniciar la aplicación.");
            System.err.println("");
            System.err.println("   Ejemplo en Linux/Mac:");
            System.err.println("   export WISHPORT_ADMIN_INITIAL_PASSWORD=TuPasswordSegura123");
            System.err.println("");
            System.err.println("   Ejemplo en Windows:");
            System.err.println("   set WISHPORT_ADMIN_INITIAL_PASSWORD=TuPasswordSegura123");
            System.err.println("═══════════════════════════════════════════════════════════════");
            System.exit(1);
        }

        // Verificar longitud mínima de la password
        if (adminInitialPassword.length() < 8) {
            System.err.println("═══════════════════════════════════════════════════════════════");
            System.err.println("❌ ERROR CRÍTICO: La contraseña en WISHPORT_ADMIN_INITIAL_PASSWORD");
            System.err.println("   debe tener al menos 8 caracteres.");
            System.err.println("═══════════════════════════════════════════════════════════════");
            System.exit(1);
        }

        // Crear el administrador inicial
        Usuario admin = new Usuario();
        admin.setNombre("Administrador");
        admin.setEmail("admin@wishport.com");
        admin.setPassword(passwordEncoder.encode(adminInitialPassword));
        admin.setTelefono("000000000");
        admin.setRol("ADMIN");

        usuarioRepository.save(admin);

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("✅ ADMINISTRADOR INICIAL CREADO AUTOMÁTICAMENTE");
        System.out.println("   Email: admin@wishport.com");
        System.out.println("   Contraseña: [definida en WISHPORT_ADMIN_INITIAL_PASSWORD]");
        System.out.println("");
        System.out.println("   IMPORTANTE: Cambia la contraseña tras el primer login.");
        System.out.println("═══════════════════════════════════════════════════════════════");
    }
}
