package com.wishport.backend.repositories;

import com.wishport.backend.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    //"SELECT * FROM usuarios WHERE email = ?"
    Usuario findByEmail(String email);
}
