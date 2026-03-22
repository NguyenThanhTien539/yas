package com.yas.search.kafka;

import static org.mockito.Mockito.verify;

import com.yas.search.kafka.config.consumer.AppKafkaListenerConfigurer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.KafkaListenerEndpointRegistrar;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class AppKafkaListenerConfigurerTest {

    @Test
    void configureKafkaListeners_shouldSetValidatorOnRegistrar() {
        LocalValidatorFactoryBean validator = org.mockito.Mockito.mock(LocalValidatorFactoryBean.class);
        KafkaListenerEndpointRegistrar registrar = org.mockito.Mockito.mock(KafkaListenerEndpointRegistrar.class);

        AppKafkaListenerConfigurer configurer = new AppKafkaListenerConfigurer(validator);

        configurer.configureKafkaListeners(registrar);

        verify(registrar).setValidator(validator);
    }
}
