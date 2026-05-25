package com.yas.search.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MessagesUtilsTest {

    @Test
    void getMessage_whenCodeExists_shouldFormatMessage() {
        String message = MessagesUtils.getMessage("PRODUCT_NOT_FOUND", 123L);

        assertEquals("The product 123 is not found", message);
    }

    @Test
    void getMessage_whenCodeDoesNotExist_shouldReturnCode() {
        String message = MessagesUtils.getMessage("UNKNOWN_MESSAGE_CODE");

        assertEquals("UNKNOWN_MESSAGE_CODE", message);
    }
}
