package project.EnterpriseSaas.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads");
        String uploadPath = uploadDir.toFile().getAbsolutePath();

        // Maps http://localhost:4000/static/** directly to the local uploads directory
        registry.addResourceHandler("/static/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
}