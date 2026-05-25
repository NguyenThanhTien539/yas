package com.yas.product.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.ResourceBundle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MessagesUtilsTest {

    @AfterEach
    void tearDown() throws Exception {
        Field messageBundleField = MessagesUtils.class.getDeclaredField("messageBundle");
        messageBundleField.setAccessible(true);
        messageBundleField.set(null, ResourceBundle.getBundle("messages.messages"));
    }

    @Test
    void getMessage_whenMessageCodeExists_shouldFormatWithArguments() {
        String result = MessagesUtils.getMessage("PRODUCT_NOT_FOUND", 99);

        assertThat(result).contains("99");
    }

    @Test
    void getMessage_whenMessageCodeDoesNotExist_shouldReturnCodeItself() {
        String result = MessagesUtils.getMessage("UNKNOWN_MESSAGE_CODE");

        assertThat(result).isEqualTo("UNKNOWN_MESSAGE_CODE");
    }
}
