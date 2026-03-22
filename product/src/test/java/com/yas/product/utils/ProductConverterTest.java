package com.yas.product.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProductConverterTest {

    @Test
    void toSlug_shouldNormalizeText() {
        String result = ProductConverter.toSlug("  Hello   Product! 2026 ");

        assertThat(result).isEqualTo("hello-product-2026");
    }

    @Test
    void toSlug_shouldTrimLeadingDash() {
        String result = ProductConverter.toSlug("@@@abc");

        assertThat(result).isEqualTo("abc");
    }
}
