package com.wishport.backend.repositories;

import com.wishport.backend.entities.Pista;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface PistaRepository extends JpaRepository<Pista, Integer> {

}