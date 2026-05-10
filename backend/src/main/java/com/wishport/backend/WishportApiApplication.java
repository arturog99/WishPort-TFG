package com.wishport.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Clase principal que arranca la aplicación Spring Boot.
 */
@EnableScheduling // Habilita la ejecución de tareas programadas (como KeepAliveTask)
@SpringBootApplication
public class WishportApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(WishportApiApplication.class, args);
	}

}
