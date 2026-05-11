package com.wishport.backend.repositories;

import com.wishport.backend.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA para la entidad Usuario.
 *
 * Al extender JpaRepository<Usuario, Integer>, hereda automáticamente
 * todos los métodos CRUD estándar sin necesidad de escribir SQL:
 *   - save(usuario)         -> INSERT o UPDATE
 *   - findById(id)          -> SELECT WHERE id_usuario = ?
 *   - findAll()             -> SELECT * FROM usuarios
 *   - deleteById(id)        -> DELETE WHERE id_usuario = ?
 *   - count()               -> SELECT COUNT(*) FROM usuarios
 *
 * Spring Data JPA genera la implementación en tiempo de ejecución.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    /**
     * Busca un usuario por su dirección de email.
     * Spring Data JPA traduce este método automáticamente a:
     *   SELECT * FROM usuarios WHERE email = ?
     *
     * Se usa principalmente en el login para verificar si el email existe
     * y obtener la contraseña hasheada para comparar con BCrypt.
     *
     * @param email Email del usuario a buscar.
     * @return El objeto Usuario si existe, o null si no se encuentra.
     */
    Usuario findByEmail(String email);
}
