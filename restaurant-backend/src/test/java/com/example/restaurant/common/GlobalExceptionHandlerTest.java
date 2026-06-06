package com.example.restaurant.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleExceptionReturnsGenericMessage() {
        ApiResponse<Void> response = handler.handleException(new RuntimeException("SQL syntax detail"));

        assertThat(response.getCode()).isEqualTo(500);
        assertThat(response.getMessage()).isEqualTo("系统繁忙，请稍后再试");
    }
}
