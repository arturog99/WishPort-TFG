package com.wishport.backend.tasks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class KeepAliveTask {

    @Autowired
    private Environment environment;

    private final RestTemplate restTemplate = new RestTemplate();

    @Scheduled(fixedRate = 300000) // Cada 5 minutos
    public void ping() {
        try {
            String port = environment.getProperty("local.server.port", environment.getProperty("server.port", "8080"));
            String url = "http://localhost:" + port + "/api/usuarios";
            restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            // Silenciar errores, el objetivo es solo mantener vivo el servlet container
        }
    }
}
