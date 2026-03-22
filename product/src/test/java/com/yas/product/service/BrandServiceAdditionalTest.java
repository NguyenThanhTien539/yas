package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Brand;
import com.yas.product.model.Product;
import com.yas.product.repository.BrandRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BrandServiceAdditionalTest {

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private BrandService brandService;

    @Test
    void getBrandsByIds_shouldMapAllBrands() {
        Brand first = new Brand();
        first.setId(1L);
        first.setName("A");
        first.setSlug("a");
        Brand second = new Brand();
        second.setId(2L);
        second.setName("B");
        second.setSlug("b");

        when(brandRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(first, second));

        var result = brandService.getBrandsByIds(List.of(1L, 2L));

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().id()).isEqualTo(1L);
        assertThat(result.get(1).slug()).isEqualTo("b");
    }

    @Test
    void delete_whenBrandNotFound_shouldThrowNotFoundException() {
        when(brandRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandService.delete(10L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void delete_whenBrandHasProducts_shouldThrowBadRequestException() {
        Brand brand = new Brand();
        brand.setId(10L);
        brand.setProducts(List.of(Product.builder().id(1L).build()));

        when(brandRepository.findById(10L)).thenReturn(Optional.of(brand));

        assertThatThrownBy(() -> brandService.delete(10L)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void delete_whenBrandHasNoProducts_shouldDeleteById() {
        Brand brand = new Brand();
        brand.setId(10L);
        brand.setProducts(List.of());

        when(brandRepository.findById(10L)).thenReturn(Optional.of(brand));

        brandService.delete(10L);

        verify(brandRepository).deleteById(10L);
    }
}
