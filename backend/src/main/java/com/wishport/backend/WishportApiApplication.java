package com.wishport.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Clase principal del backend WishPort.
 *
 * @SpringBootApplication es una anotación compuesta que activa:
 *   - @Configuration: esta clase puede definir Beans de Spring.
 *   - @EnableAutoConfiguration: configura Spring Boot automáticamente
 *     según las dependencias del classpath (JPA, Security, Web...).
 *   - @ComponentScan: escanea todos los paquetes del proyecto buscando
 *     @Component, @Service, @Repository, @Controller, etc.
 *
 * @EnableScheduling habilita el soporte para tareas programadas con @Scheduled,
 * necesario para que KeepAliveTask.ping() se ejecute cada 5 minutos.
 */
@EnableScheduling
@SpringBootApplication
public class WishportApiApplication {

	/**
	 * Punto de entrada de la aplicación.
	 * SpringApplication.run() arranca el contenedor de Spring,
	 * configura Tomcat embebido y pone en marcha todos los componentes.
	 * @param args Argumentos de línea de comandos (no usados en esta app).
	 */
	public static void main(String[] args) {
		SpringApplication.run(WishportApiApplication.class, args);
	}

}
