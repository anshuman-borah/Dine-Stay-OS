package project.EnterpriseSaas.demo.modules.storage.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class StorageWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // This maps the frontend URL pattern "/static/**" to the actual physical "uploads" directory
        String uploadsPath = Paths.get(System.getProperty("user.dir"), "uploads").toUri().toString();

        registry.addResourceHandler("/static/**")
                .addResourceLocations(uploadsPath);
    }
}