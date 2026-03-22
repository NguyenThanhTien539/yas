package com.yas.search.constant;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MessageCodeTest {

    @Test
    void constants_shouldMatchExpectedValues() {
        assertEquals("PRODUCT_NOT_FOUND", MessageCode.PRODUCT_NOT_FOUND);
    }
}
