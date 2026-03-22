package com.yas.inventory.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AbstractCircuitBreakFallbackHandlerTest {

    private final AbstractCircuitBreakFallbackHandler handler = new AbstractCircuitBreakFallbackHandler() {
    };

    @Test
    void handleBodilessFallback_shouldRethrowOriginalException() {
        RuntimeException ex = new RuntimeException("fallback-error");

        assertThrows(RuntimeException.class, () -> handler.handleBodilessFallback(ex));
    }

    @Test
    void handleTypedFallback_shouldRethrowOriginalException() {
        IllegalStateException ex = new IllegalStateException("typed-fallback-error");

        assertThrows(IllegalStateException.class, () -> handler.handleTypedFallback(ex));
    }
}
