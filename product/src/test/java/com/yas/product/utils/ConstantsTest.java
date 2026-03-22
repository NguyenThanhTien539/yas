package com.yas.product.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConstantsTest {

    @Test
    void errorCodeConstants_shouldBeAccessible() {
        Constants constants = new Constants();
        Constants.ErrorCode errorCode = constants.new ErrorCode();

        assertThat(errorCode).isNotNull();
        assertThat(Constants.ErrorCode.PRODUCT_NOT_FOUND).isEqualTo("PRODUCT_NOT_FOUND");
        assertThat(Constants.ErrorCode.BRAND_NOT_FOUND).isEqualTo("BRAND_NOT_FOUND");
        assertThat(Constants.ErrorCode.CATEGORY_NOT_FOUND).isEqualTo("CATEGORY_NOT_FOUND");
    }
}
