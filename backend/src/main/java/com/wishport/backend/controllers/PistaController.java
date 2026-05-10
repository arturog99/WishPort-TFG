package com.wishport.backend.controllers;

import com.wishport.backend.entities.Pista;
import com.wishport.backend.repositories.PistaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador para gestionar las operaciones relacionadas con las pistas.
 */
@RestController
@RequestMapping("/api/pistas")
public class PistaController {

    @Autowired
    private PistaRepository pistaRepository;

    /**
     * Obtiene la lista de todas las pistas disponibles en la base de datos.
     * Es público, cualquiera puede ver las pistas sin estar registrado.
     * @return Lista de pistas en formato JSON.
     */
    @GetMapping
    public List<Pista> obtenerTodasLasPistas() {
        return pistaRepository.findAll();
    }
}
