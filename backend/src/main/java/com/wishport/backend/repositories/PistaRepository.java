package com.wishport.backend.repositories;

import com.wishport.backend.entities.Pista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA para la entidad Pista.
 *
 * Al extender JpaRepository<Pista, Integer>, Spring Data JPA
 * genera automáticamente en tiempo de ejecución todos los métodos
 * CRUD estándar sin necesidad de escribir SQL:
 *   - findAll()      -> SELECT * FROM pistas  (usado en GET /api/pistas)
 *   - findById(id)   -> SELECT * FROM pistas WHERE id_pista = ?
 *   - save(pista)    -> INSERT o UPDATE
 *   - deleteById(id) -> DELETE WHERE id_pista = ?
 *
 * Por ahora no se necesitan consultas personalizadas para las pistas,
 * ya que solo se expone el listado completo al frontend.
 */
@Repository
public interface PistaRepository extends JpaRepository<Pista, Integer> {

}
