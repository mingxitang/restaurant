package com.example.restaurant.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {
    @Test
    void validateRejectsMissingSecret() {
        JwtProperties properties = properties(null, 1440L);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT_SECRET must be configured");
    }

    @Test
    void validateRejectsOldInsecureDefaultSecret() {
        JwtProperties properties = properties("restaurant-course-design-secret-key-change-me-please-2026", 1440L);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT_SECRET must not use the old insecure default value");
    }

    @Test
    void validateRejectsShortSecret() {
        JwtProperties properties = properties("too-short", 1440L);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT_SECRET must be at least 32 bytes");
    }

    @Test
    void validateAcceptsSecureSecret() {
        JwtProperties properties = properties("abcdefghijklmnopqrstuvwxyz123456", 1440L);

        assertThatCode(properties::validate).doesNotThrowAnyException();
    }

    private JwtProperties properties(String secret, Long expirationMinutes) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secret);
        properties.setExpirationMinutes(expirationMinutes);
        return properties;
    }
}
