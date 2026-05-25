package com.yas.product.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PriceValidatorTest {

    private PriceValidator priceValidator;

    @BeforeEach
    void setUp() {
        priceValidator = new PriceValidator();
    }

    @Test
    void isValid_whenPriceIsZero_shouldReturnTrue() {
        assertThat(priceValidator.isValid(0D, null)).isTrue();
    }

    @Test
    void isValid_whenPriceIsPositive_shouldReturnTrue() {
        assertThat(priceValidator.isValid(19.99D, null)).isTrue();
    }

    @Test
    void isValid_whenPriceIsNegative_shouldReturnFalse() {
        assertThat(priceValidator.isValid(-1D, null)).isFalse();
    }
}
