package com.yas.search.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.search.config.ServiceUrlConfig;
import com.yas.search.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class ProductSyncDataServiceAdditionalTest {

    private ProductRepository productRepository;
    private ProductSyncDataService productSyncDataService;

    @BeforeEach
    void setUp() {
        productRepository = org.mockito.Mockito.mock(ProductRepository.class);
        RestClient restClient = org.mockito.Mockito.mock(RestClient.class);
        ServiceUrlConfig serviceUrlConfig = org.mockito.Mockito.mock(ServiceUrlConfig.class);
        productSyncDataService = new ProductSyncDataService(restClient, serviceUrlConfig, productRepository);
    }

    @Test
    void deleteProduct_whenProductDoesNotExist_shouldNotDelete() {
        Long productId = 99L;
        when(productRepository.existsById(productId)).thenReturn(false);

        productSyncDataService.deleteProduct(productId);

        verify(productRepository, never()).deleteById(productId);
    }
}
