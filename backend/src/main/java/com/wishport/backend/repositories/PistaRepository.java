package com.wishport.backend.repositories;

import com.wishport.backend.entities.Pista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la entidad Pista.
 * Otorga los métodos básicos para obtener o guardar pistas en base de datos.
 */
@Repository
public interface PistaRepository extends JpaRepository<Pista, Integer> {

}
