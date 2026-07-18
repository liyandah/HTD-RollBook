package org.salvationarmy.whatsapp.config;

import org.salvationarmy.whatsapp.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private FileService fileService;

    // CORS is handled by CorsConfig bean for Spring Security
    // Removing duplicate CORS configuration to avoid conflicts

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadLocation = "file:uploads/";
        try {
            uploadLocation = "file:" + fileService.getUploadPath().toString().replace("\\", "/") + "/";
        } catch (IOException ignored) {
            // Fall back to default relative uploads directory mapping.
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation);
        registry.addResourceHandler("/api/images/**")
                .addResourceLocations(uploadLocation);
    }
}






