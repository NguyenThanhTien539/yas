package com.yas.tax.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AbstractCircuitBreakFallbackHandlerTest {

    private final AbstractCircuitBreakFallbackHandler handler = new AbstractCircuitBreakFallbackHandler() {
    };

    @Test
    void handleBodilessFallback_shouldRethrow() {
        RuntimeException ex = new RuntimeException("error");
        assertThrows(RuntimeException.class, () -> handler.handleBodilessFallback(ex));
    }

    @Test
    void handleTypedFallback_shouldRethrow() {
        IllegalStateException ex = new IllegalStateException("typed-error");
        assertThrows(IllegalStateException.class, () -> handler.handleTypedFallback(ex));
    }
}
