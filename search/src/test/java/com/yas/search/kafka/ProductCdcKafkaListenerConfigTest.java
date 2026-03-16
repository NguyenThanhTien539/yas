package com.yas.search.kafka;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.yas.search.kafka.config.consumer.ProductCdcKafkaListenerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;

class ProductCdcKafkaListenerConfigTest {

    @Test
    void listenerContainerFactory_shouldNotBeNull() {
        ProductCdcKafkaListenerConfig config = new ProductCdcKafkaListenerConfig(new KafkaProperties());

        assertNotNull(config.listenerContainerFactory());
    }
}
