package com.wishport.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración MVC general del backend.
 *
 * Permite que el servidor sirva archivos estáticos (imágenes de pistas)
 * directamente desde la carpeta src/main/resources/static/images/.
 *
 * Sin esta configuración, las peticiones GET /images/padel.jpg
 * devolverían 404 aunque el archivo existiera en el classpath.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Registra un manejador de recursos estáticos.
     *
     * Mapeo:
     *   URL: /images/**
     *   Ruta física: classpath:/static/images/
     *   (es decir, src/main/resources/static/images/)
     *
     * Ejemplo:
     *   GET https://api.wishport.com/images/padel.jpg
     *   -> Sirve el archivo src/main/resources/static/images/padel.jpg
     *
     * En SecurityConfig esta ruta está marcada como pública (.permitAll())
     * para que las imágenes sean accesibles sin token JWT.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
    }
}
