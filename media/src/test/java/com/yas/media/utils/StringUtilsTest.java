package com.yas.media.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StringUtilsTest {

    @Test
    void hasText_whenNull_thenReturnFalse() {
        assertThat(StringUtils.hasText(null)).isFalse();
    }

    @Test
    void hasText_whenEmpty_thenReturnFalse() {
        assertThat(StringUtils.hasText("")).isFalse();
    }

    @Test
    void hasText_whenBlank_thenReturnFalse() {
        assertThat(StringUtils.hasText("   ")).isFalse();
    }

    @Test
    void hasText_whenHasText_thenReturnTrue() {
        assertThat(StringUtils.hasText("hello")).isTrue();
    }
}
