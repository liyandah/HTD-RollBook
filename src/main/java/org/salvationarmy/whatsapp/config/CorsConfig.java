package org.salvationarmy.whatsapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Split allowed origins from config (trim whitespace)
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        
        // Use allowedOriginPatterns to support both exact origins and patterns
        // This is required when allowCredentials is true
        List<String> originPatterns = new java.util.ArrayList<>(origins);
        
        // Add pattern support for local network IPs
        originPatterns.add("http://192.168.*:*");
        originPatterns.add("http://10.*:*");
        originPatterns.add("http://172.*:*");
        
        // Add pattern support for ngrok domains
        originPatterns.add("https://*.ngrok-free.app");
        originPatterns.add("https://*.ngrok-free.dev");
        originPatterns.add("https://*.ngrok.io");
        originPatterns.add("https://*.ngrok.app");
        
        configuration.setAllowedOriginPatterns(originPatterns);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        // Log CORS configuration for debugging
        System.out.println("[CORS] Allowed origin patterns: " + originPatterns);
        System.out.println("[CORS] Allowed methods: GET, POST, PUT, PATCH, DELETE, OPTIONS");
        System.out.println("[CORS] Allow credentials: true");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}






