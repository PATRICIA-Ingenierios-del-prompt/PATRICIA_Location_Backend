package ingprompt.patricia.location.infrastructure.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

/**
 * Rejects startup when the encryption secret is a known weak/default value
 * and the active profile is production-like.
 */
@Slf4j
@Component
public class EncryptionSecretGuard {

    private static final Set<String> WEAK_SECRETS = Set.of(
            "dev-only-change-me-in-production",
            "changeme",
            "secret",
            "password"
    );

    private static final Set<String> PRODUCTION_PROFILES = Set.of(
            "prod", "production", "staging"
    );

    private final String secret;
    private final Environment environment;

    public EncryptionSecretGuard(
            @Value("${location.encryption.secret}") String secret,
            Environment environment) {
        this.secret = secret;
        this.environment = environment;
    }

    @PostConstruct
    void validate() {
        boolean isProduction = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> PRODUCTION_PROFILES.contains(p.toLowerCase()));

        if (WEAK_SECRETS.contains(secret.toLowerCase())) {
            if (isProduction) {
                throw new IllegalStateException(
                        "LOCATION_ENCRYPTION_SECRET is set to a known weak default. "
                                + "Set a strong, random 256-bit secret via environment variable or vault before starting in production.");
            }
            log.warn("LOCATION_ENCRYPTION_SECRET is a weak default. Acceptable for local dev only.");
        }

        if (secret.length() < 16) {
            log.warn("LOCATION_ENCRYPTION_SECRET is shorter than 16 characters; consider using a stronger secret.");
        }
    }
}
