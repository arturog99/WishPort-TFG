package com.wishport.backend.tasks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Tarea programada que se ejecuta periódicamente para evitar que el servidor 
 * se suspenda por inactividad (útil si está alojado en un servidor gratuito).
 */
@Component
public class KeepAliveTask {

    @Autowired
    private Environment environment;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Hace una petición GET a la propia API (al endpoint de usuarios) cada 5 minutos (300.000 ms).
     * El objetivo no es procesar el resultado, sino mantener el contenedor de servlets activo.
     */
    @Scheduled(fixedRate = 300000) 
    public void ping() {
        try {
            // Intenta obtener el puerto configurado o usa 8080 por defecto
            String port = environment.getProperty("local.server.port", environment.getProperty("server.port", "8080"));
            String url = "http://localhost:" + port + "/api/pistas"; // Usamos 'pistas' por ser un endpoint público
            
            restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            // Se silencian los errores intencionadamente; 
            // el objetivo es solo realizar la petición de red
        }
    }
}
