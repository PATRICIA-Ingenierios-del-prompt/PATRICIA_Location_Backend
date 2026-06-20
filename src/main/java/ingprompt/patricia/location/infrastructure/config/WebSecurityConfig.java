package ingprompt.patricia.location.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Explicit CORS policy. Defaults to the API gateway origin only;
 * override via CORS_ALLOWED_ORIGINS (comma-separated) for local dev.
 */
@Configuration
public class WebSecurityConfig {

    @Value("${cors.allowed-origins:}")
    private String allowedOrigins;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                String[] origins = resolveOrigins();
                registry.addMapping("/api/**")
                        .allowedOrigins(origins)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("Content-Type", "Authorization", "X-User-Id", "X-User-Role")
                        .exposedHeaders("Location")
                        .allowCredentials(origins.length > 0 && !origins[0].equals("*"))
                        .maxAge(3600);
            }
        };
    }

    private String[] resolveOrigins() {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            return new String[0];
        }
        return allowedOrigins.split(",");
    }
}
