package com.example.restaurant.security;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private static final String INSECURE_DEFAULT_SECRET = "restaurant-course-design-secret-key-change-me-please-2026";
    private static final int MIN_SECRET_BYTES = 32;

    private String secret;
    private Long expirationMinutes;

    @PostConstruct
    public void validate() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured");
        }
        if (INSECURE_DEFAULT_SECRET.equals(secret)) {
            throw new IllegalStateException("JWT_SECRET must not use the old insecure default value");
        }
        if (secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes");
        }
        if (expirationMinutes == null || expirationMinutes <= 0) {
            throw new IllegalStateException("jwt.expiration-minutes must be positive");
        }
    }
}
