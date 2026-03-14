package com.wishport.backend.controllers;

import com.wishport.backend.entities.Pista;
import com.wishport.backend.repositories.PistaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pistas")
public class PistaController {

    @Autowired
    private PistaRepository pistaRepository;

    // Cuando alguien entre a /api/pistas, este método se ejecuta
    @GetMapping
    public List<Pista> obtenerTodasLasPistas() {
        // Esto hace un "SELECT * FROM pistas" automático y lo transforma a JSON
        return pistaRepository.findAll();
    }
}