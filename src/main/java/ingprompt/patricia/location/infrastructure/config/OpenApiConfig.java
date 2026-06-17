package ingprompt.patricia.location.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI locationOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Location API")
                .description("Real-time event geolocation with encrypted, law-compliant retention (Ley 1581).")
                .version("v0.0.1")
                .contact(new Contact().name("PATRICIA - Location")));
    }
}
