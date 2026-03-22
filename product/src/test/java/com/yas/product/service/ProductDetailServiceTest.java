package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Brand;
import com.yas.product.model.Category;
import com.yas.product.model.Product;
import com.yas.product.model.ProductCategory;
import com.yas.product.model.ProductImage;
import com.yas.product.model.ProductOption;
import com.yas.product.model.ProductOptionCombination;
import com.yas.product.repository.ProductOptionCombinationRepository;
import com.yas.product.repository.ProductRepository;
import com.yas.product.viewmodel.NoFileMediaVm;
import com.yas.product.viewmodel.product.ProductDetailInfoVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductDetailServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MediaService mediaService;

    @Mock
    private ProductOptionCombinationRepository productOptionCombinationRepository;

    @InjectMocks
    private ProductDetailService productDetailService;

    @Test
    void getProductDetailById_whenProductNotFound_shouldThrowNotFoundException() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productDetailService.getProductDetailById(1L))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getProductDetailById_whenProductUnpublished_shouldThrowNotFoundException() {
        Product unpublishedProduct = Product.builder().id(2L).isPublished(false).build();
        when(productRepository.findById(2L)).thenReturn(Optional.of(unpublishedProduct));

        assertThatThrownBy(() -> productDetailService.getProductDetailById(2L))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getProductDetailById_whenPublishedProductHasNoOptions_shouldReturnDetail() {
        Brand brand = new Brand();
        brand.setId(10L);
        brand.setName("Brand A");

        Category category = new Category();
        category.setId(20L);
        category.setName("Category A");

        ProductCategory productCategory = ProductCategory.builder().category(category).build();
        ProductImage productImage = ProductImage.builder().imageId(301L).build();

        Product product = Product.builder()
            .id(100L)
            .name("Main Product")
            .shortDescription("short")
            .description("desc")
            .specification("spec")
            .sku("SKU-1")
            .gtin("GTIN-1")
            .slug("main-product")
            .price(99.9)
            .isAllowedToOrder(true)
            .isPublished(true)
            .isFeatured(true)
            .isVisibleIndividually(true)
            .stockTrackingEnabled(true)
            .taxClassId(1L)
            .brand(brand)
            .thumbnailMediaId(300L)
            .hasOptions(false)
            .productCategories(List.of(productCategory))
            .productImages(List.of(productImage))
            .build();

        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(mediaService.getMedia(300L)).thenReturn(new NoFileMediaVm(300L, "", "", "", "https://thumb"));
        when(mediaService.getMedia(301L)).thenReturn(new NoFileMediaVm(301L, "", "", "", "https://img-1"));

        ProductDetailInfoVm result = productDetailService.getProductDetailById(100L);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getBrandId()).isEqualTo(10L);
        assertThat(result.getBrandName()).isEqualTo("Brand A");
        assertThat(result.getCategories()).hasSize(1);
        assertThat(result.getVariations()).isEmpty();
        assertThat(result.getThumbnail().url()).isEqualTo("https://thumb");
        assertThat(result.getProductImages()).hasSize(1);
        assertThat(result.getProductImages().getFirst().url()).isEqualTo("https://img-1");
    }

    @Test
    void getProductDetailById_whenHasOptions_shouldReturnOnlyPublishedVariations() {
        ProductOption colorOption = new ProductOption();
        colorOption.setId(501L);

        Product publishedVariation = Product.builder()
            .id(201L)
            .name("Variation 1")
            .slug("variation-1")
            .sku("SKU-V1")
            .gtin("GTIN-V1")
            .price(120D)
            .isPublished(true)
            .thumbnailMediaId(401L)
            .productImages(List.of(ProductImage.builder().imageId(402L).build()))
            .build();

        Product unpublishedVariation = Product.builder()
            .id(202L)
            .name("Variation 2")
            .slug("variation-2")
            .sku("SKU-V2")
            .gtin("GTIN-V2")
            .price(130D)
            .isPublished(false)
            .build();

        Product product = Product.builder()
            .id(200L)
            .name("Main Product")
            .slug("main-product")
            .sku("SKU-MAIN")
            .gtin("GTIN-MAIN")
            .price(200D)
            .isAllowedToOrder(true)
            .isPublished(true)
            .isFeatured(false)
            .isVisibleIndividually(true)
            .stockTrackingEnabled(true)
            .hasOptions(true)
            .products(List.of(publishedVariation, unpublishedVariation))
            .build();

        ProductOptionCombination combination = ProductOptionCombination.builder()
            .product(publishedVariation)
            .productOption(colorOption)
            .value("Red")
            .displayOrder(1)
            .build();

        when(productRepository.findById(200L)).thenReturn(Optional.of(product));
        when(productOptionCombinationRepository.findAllByProduct(publishedVariation)).thenReturn(List.of(combination));
        when(mediaService.getMedia(401L)).thenReturn(new NoFileMediaVm(401L, "", "", "", "https://v-thumb"));
        when(mediaService.getMedia(402L)).thenReturn(new NoFileMediaVm(402L, "", "", "", "https://v-img"));

        ProductDetailInfoVm result = productDetailService.getProductDetailById(200L);

        assertThat(result.getVariations()).hasSize(1);
        assertThat(result.getVariations().getFirst().id()).isEqualTo(201L);
        assertThat(result.getVariations().getFirst().options()).containsEntry(501L, "Red");
        verify(productOptionCombinationRepository).findAllByProduct(publishedVariation);
    }
}
