package com.yas.search.consumer;

import static com.yas.commonlibrary.kafka.cdc.message.Operation.DELETE;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.yas.commonlibrary.kafka.cdc.message.ProductCdcMessage;
import com.yas.commonlibrary.kafka.cdc.message.ProductMsgKey;
import com.yas.search.kafka.consumer.ProductSyncDataConsumer;
import com.yas.search.service.ProductSyncDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ProductSyncDataConsumerAdditionalTest {

    @InjectMocks
    private ProductSyncDataConsumer productSyncDataConsumer;

    @Mock
    private ProductSyncDataService productSyncDataService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void sync_whenCdcMessageIsNull_shouldDeleteProduct() {
        long productId = 10L;

        productSyncDataConsumer.sync(ProductMsgKey.builder().id(productId).build(), null);

        verify(productSyncDataService, times(1)).deleteProduct(productId);
    }

    @Test
    void sync_whenDeleteOperation_shouldDeleteProduct() {
        long productId = 11L;

        productSyncDataConsumer.sync(
            ProductMsgKey.builder().id(productId).build(),
            ProductCdcMessage.builder().op(DELETE).build()
        );

        verify(productSyncDataService, times(1)).deleteProduct(productId);
    }
}
