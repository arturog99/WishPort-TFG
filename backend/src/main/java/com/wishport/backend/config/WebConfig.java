package com.wishport.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración general web.
 * Permite servir archivos estáticos (imágenes) al cliente.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Mapea la ruta "/images/**" a la carpeta de recursos estáticos "/static/images/".
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
    }
}
