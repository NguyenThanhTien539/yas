package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import com.yas.product.model.ProductRelated;
import com.yas.product.model.attribute.ProductAttribute;
import com.yas.product.model.attribute.ProductAttributeGroup;
import com.yas.product.model.attribute.ProductAttributeValue;
import com.yas.product.model.enumeration.FilterExistInWhSelection;
import com.yas.product.model.enumeration.DimensionUnit;
import com.yas.product.repository.BrandRepository;
import com.yas.product.repository.CategoryRepository;
import com.yas.product.repository.ProductCategoryRepository;
import com.yas.product.repository.ProductImageRepository;
import com.yas.product.repository.ProductOptionCombinationRepository;
import com.yas.product.repository.ProductOptionRepository;
import com.yas.product.repository.ProductOptionValueRepository;
import com.yas.product.repository.ProductRelatedRepository;
import com.yas.product.repository.ProductRepository;
import com.yas.product.viewmodel.NoFileMediaVm;
import com.yas.product.viewmodel.product.ProductDetailGetVm;
import com.yas.product.viewmodel.product.ProductDetailVm;
import com.yas.product.viewmodel.product.ProductEsDetailVm;
import com.yas.product.viewmodel.product.ProductFeatureGetVm;
import com.yas.product.viewmodel.product.ProductGetDetailVm;
import com.yas.product.viewmodel.product.ProductInfoVm;
import com.yas.product.viewmodel.product.ProductListVm;
import com.yas.product.viewmodel.product.ProductOptionValueDisplay;
import com.yas.product.viewmodel.product.ProductPostVm;
import com.yas.product.viewmodel.product.ProductQuantityPostVm;
import com.yas.product.viewmodel.product.ProductQuantityPutVm;
import com.yas.product.viewmodel.product.ProductPutVm;
import com.yas.product.viewmodel.product.ProductSlugGetVm;
import com.yas.product.viewmodel.product.ProductThumbnailGetVm;
import com.yas.product.viewmodel.product.ProductThumbnailVm;
import com.yas.product.viewmodel.product.ProductVariationPostVm;
import com.yas.product.viewmodel.product.ProductVariationPutVm;
import com.yas.product.viewmodel.product.ProductVariationGetVm;
import com.yas.product.viewmodel.product.ProductsGetVm;
import com.yas.product.viewmodel.productoption.ProductOptionValuePostVm;
import com.yas.product.viewmodel.productoption.ProductOptionValuePutVm;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.InternalServerErrorException;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private MediaService mediaService;
    @Mock
    private BrandRepository brandRepository;
    @Mock
    private ProductCategoryRepository productCategoryRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductImageRepository productImageRepository;
    @Mock
    private ProductOptionRepository productOptionRepository;
    @Mock
    private ProductOptionValueRepository productOptionValueRepository;
    @Mock
    private ProductOptionCombinationRepository productOptionCombinationRepository;
    @Mock
    private ProductRelatedRepository productRelatedRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void getLatestProducts_whenCountIsZero_shouldReturnEmptyList() {
        List<ProductListVm> result = productService.getLatestProducts(0);

        assertThat(result).isEmpty();
        verify(productRepository, never()).getLatestProducts(any(PageRequest.class));
    }

    @Test
    void getLatestProducts_whenProductsExist_shouldMapToVm() {
        Product product = Product.builder()
            .id(1L)
            .name("Product A")
            .slug("product-a")
            .isAllowedToOrder(true)
            .isPublished(true)
            .isFeatured(false)
            .isVisibleIndividually(true)
            .price(99.0)
            .taxClassId(2L)
            .build();

        when(productRepository.getLatestProducts(any(PageRequest.class))).thenReturn(List.of(product));

        List<ProductListVm> result = productService.getLatestProducts(3);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(1L);
        assertThat(result.getFirst().name()).isEqualTo("Product A");
    }

    @Test
    void getProductSlug_whenProductIsVariant_shouldReturnParentSlugAndVariantId() {
        Product parent = Product.builder().id(10L).slug("parent-slug").build();
        Product variant = Product.builder().id(11L).slug("variant-slug").parent(parent).build();

        when(productRepository.findById(11L)).thenReturn(Optional.of(variant));

        ProductSlugGetVm result = productService.getProductSlug(11L);

        assertThat(result.slug()).isEqualTo("parent-slug");
        assertThat(result.productVariantId()).isEqualTo(11L);
    }

    @Test
    void getProductSlug_whenProductIsMain_shouldReturnOwnSlugAndNullVariantId() {
        Product product = Product.builder().id(12L).slug("main-slug").build();

        when(productRepository.findById(12L)).thenReturn(Optional.of(product));

        ProductSlugGetVm result = productService.getProductSlug(12L);

        assertThat(result.slug()).isEqualTo("main-slug");
        assertThat(result.productVariantId()).isNull();
    }

    @Test
    void setProductImages_whenImageIdsEmpty_shouldDeleteAllByProductId() {
        Product product = Product.builder().id(20L).productImages(List.of()).build();

        List<ProductImage> result = productService.setProductImages(List.of(), product);

        assertThat(result).isEmpty();
        verify(productImageRepository).deleteByProductId(20L);
    }

    @Test
    void setProductImages_whenExistingImagesChanged_shouldReturnNewAndDeleteRemovedImages() {
        ProductImage oldImage1 = ProductImage.builder().imageId(1L).build();
        ProductImage oldImage2 = ProductImage.builder().imageId(2L).build();
        Product product = Product.builder().id(21L).productImages(List.of(oldImage1, oldImage2)).build();

        List<ProductImage> result = productService.setProductImages(List.of(2L, 3L), product);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getImageId()).isEqualTo(3L);
        verify(productImageRepository).deleteByImageIdInAndProductId(List.of(1L), 21L);
    }

    @Test
    void getProductsByBrand_whenBrandNotFound_shouldThrowNotFoundException() {
        when(brandRepository.findBySlug("missing-brand")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductsByBrand("missing-brand"))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getProductsByBrand_whenBrandExists_shouldReturnThumbnails() {
        Brand brand = new Brand();
        brand.setId(1L);
        brand.setName("Brand A");

        Product product = Product.builder()
            .id(100L)
            .name("Product A")
            .slug("product-a")
            .thumbnailMediaId(500L)
            .build();

        when(brandRepository.findBySlug("brand-a")).thenReturn(Optional.of(brand));
        when(productRepository.findAllByBrandAndIsPublishedTrueOrderByIdAsc(brand)).thenReturn(List.of(product));
        when(mediaService.getMedia(500L)).thenReturn(new NoFileMediaVm(500L, "", "", "", "https://img"));

        List<ProductThumbnailVm> result = productService.getProductsByBrand("brand-a");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(100L);
        assertThat(result.getFirst().thumbnailUrl()).isEqualTo("https://img");
    }

    @Test
    void getFeaturedProductsById_whenProductThumbnailIsEmpty_shouldFallbackToParentThumbnail() {
        Product parent = Product.builder().id(90L).thumbnailMediaId(900L).build();
        Product variant = Product.builder()
            .id(91L)
            .name("Variant")
            .slug("variant")
            .price(50D)
            .thumbnailMediaId(901L)
            .parent(parent)
            .build();

        when(productRepository.findAllByIdIn(List.of(91L))).thenReturn(List.of(variant));
        when(mediaService.getMedia(901L)).thenReturn(new NoFileMediaVm(901L, "", "", "", ""));
        when(productRepository.findById(90L)).thenReturn(Optional.of(parent));
        when(mediaService.getMedia(900L)).thenReturn(new NoFileMediaVm(900L, "", "", "", "https://parent"));

        List<ProductThumbnailGetVm> result = productService.getFeaturedProductsById(List.of(91L));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().thumbnailUrl()).isEqualTo("https://parent");
    }

    @Test
    void getProductVariationsByParentId_whenHasNoOptions_shouldReturnEmpty() {
        Product parent = Product.builder().id(300L).hasOptions(false).build();
        when(productRepository.findById(300L)).thenReturn(Optional.of(parent));

        List<ProductVariationGetVm> result = productService.getProductVariationsByParentId(300L);

        assertThat(result).isEmpty();
    }

    @Test
    void getProductVariationsByParentId_whenHasOptions_shouldReturnPublishedVariations() {
        ProductOption option = new ProductOption();
        option.setId(700L);

        Product publishedVariation = Product.builder()
            .id(301L)
            .name("V1")
            .slug("v1")
            .sku("SKU-V1")
            .gtin("GTIN-V1")
            .price(10D)
            .isPublished(true)
            .thumbnailMediaId(1001L)
            .productImages(List.of(ProductImage.builder().imageId(1002L).build()))
            .build();

        Product unpublishedVariation = Product.builder()
            .id(302L)
            .name("V2")
            .isPublished(false)
            .productImages(List.of())
            .build();

        Product parent = Product.builder()
            .id(300L)
            .hasOptions(true)
            .products(List.of(publishedVariation, unpublishedVariation))
            .build();

        ProductOptionCombination combination = ProductOptionCombination.builder()
            .product(publishedVariation)
            .productOption(option)
            .value("Blue")
            .displayOrder(1)
            .build();

        when(productRepository.findById(300L)).thenReturn(Optional.of(parent));
        when(productOptionCombinationRepository.findAllByProduct(publishedVariation)).thenReturn(List.of(combination));
        when(mediaService.getMedia(1001L)).thenReturn(new NoFileMediaVm(1001L, "", "", "", "https://thumb"));
        when(mediaService.getMedia(1002L)).thenReturn(new NoFileMediaVm(1002L, "", "", "", "https://img"));

        List<ProductVariationGetVm> result = productService.getProductVariationsByParentId(300L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(301L);
        assertThat(result.getFirst().options()).containsEntry(700L, "Blue");
        assertThat(result.getFirst().thumbnail().url()).isEqualTo("https://thumb");
    }

    @Test
    void updateProductQuantity_shouldUpdateStockByMatchingProductIds() {
        Product first = Product.builder().id(1L).stockQuantity(1L).build();
        Product second = Product.builder().id(2L).stockQuantity(2L).build();

        List<ProductQuantityPostVm> updates = List.of(
            new ProductQuantityPostVm(1L, 10L),
            new ProductQuantityPostVm(2L, 20L)
        );

        when(productRepository.findAllByIdIn(List.of(1L, 2L))).thenReturn(List.of(first, second));

        productService.updateProductQuantity(updates);

        assertThat(first.getStockQuantity()).isEqualTo(10L);
        assertThat(second.getStockQuantity()).isEqualTo(20L);
        verify(productRepository).saveAll(List.of(first, second));
    }

    @Test
    void subtractStockQuantity_shouldNotDropBelowZero_andMergeDuplicateInputs() {
        Product tracked = Product.builder().id(10L).stockTrackingEnabled(true).stockQuantity(7L).build();
        Product notTracked = Product.builder().id(11L).stockTrackingEnabled(false).stockQuantity(9L).build();

        List<ProductQuantityPutVm> updates = List.of(
            new ProductQuantityPutVm(10L, 3L),
            new ProductQuantityPutVm(10L, 10L),
            new ProductQuantityPutVm(11L, 5L)
        );

        when(productRepository.findAllByIdIn(List.of(10L, 10L, 11L))).thenReturn(List.of(tracked, notTracked));

        productService.subtractStockQuantity(updates);

        assertThat(tracked.getStockQuantity()).isZero();
        assertThat(notTracked.getStockQuantity()).isEqualTo(9L);
        verify(productRepository).saveAll(List.of(tracked, notTracked));
    }

    @Test
    void restoreStockQuantity_shouldIncreaseQuantity_andMergeDuplicateInputs() {
        Product tracked = Product.builder().id(20L).stockTrackingEnabled(true).stockQuantity(5L).build();

        List<ProductQuantityPutVm> updates = List.of(
            new ProductQuantityPutVm(20L, 2L),
            new ProductQuantityPutVm(20L, 4L)
        );

        when(productRepository.findAllByIdIn(List.of(20L, 20L))).thenReturn(List.of(tracked));

        productService.restoreStockQuantity(updates);

        assertThat(tracked.getStockQuantity()).isEqualTo(11L);
        verify(productRepository).saveAll(List.of(tracked));
    }

    @Test
    void getProductsByMultiQuery_shouldReturnPagedResult() {
        Product product = Product.builder()
            .id(500L)
            .name("Phone")
            .slug("phone")
            .price(1000D)
            .thumbnailMediaId(1000L)
            .build();

        Page<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 5), 1);
        when(productRepository.findByProductNameAndCategorySlugAndPriceBetween(
            "phone", "electronics", 10D, 2000D, PageRequest.of(0, 5))).thenReturn(page);
        when(mediaService.getMedia(1000L)).thenReturn(new NoFileMediaVm(1000L, "", "", "", "https://phone"));

        ProductsGetVm result = productService.getProductsByMultiQuery(0, 5, "Phone", "electronics", 10D, 2000D);

        assertThat(result.productContent()).hasSize(1);
        assertThat(result.productContent().getFirst().id()).isEqualTo(500L);
        assertThat(result.pageNo()).isZero();
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void getRelatedProductsBackoffice_shouldMapRelatedProducts() {
        Product related = Product.builder()
            .id(900L)
            .name("Related")
            .slug("related")
            .isAllowedToOrder(true)
            .isPublished(true)
            .isFeatured(false)
            .isVisibleIndividually(true)
            .price(99D)
            .taxClassId(1L)
            .build();
        Product owner = Product.builder()
            .id(901L)
            .relatedProducts(List.of(ProductRelated.builder().relatedProduct(related).build()))
            .build();

        when(productRepository.findById(901L)).thenReturn(Optional.of(owner));

        List<ProductListVm> result = productService.getRelatedProductsBackoffice(901L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(900L);
        assertThat(result.getFirst().slug()).isEqualTo("related");
    }

    @Test
    void getRelatedProductsStorefront_shouldReturnOnlyPublishedRelatedProducts() {
        Product published = Product.builder().id(100L).name("P1").slug("p1").isPublished(true)
            .thumbnailMediaId(1001L).price(11D).build();
        Product unpublished = Product.builder().id(101L).name("P2").slug("p2").isPublished(false)
            .thumbnailMediaId(1002L).price(12D).build();

        Product owner = Product.builder().id(200L).build();
        ProductRelated rel1 = ProductRelated.builder().product(owner).relatedProduct(published).build();
        ProductRelated rel2 = ProductRelated.builder().product(owner).relatedProduct(unpublished).build();
        Page<ProductRelated> page = new PageImpl<>(List.of(rel1, rel2), PageRequest.of(0, 10), 2);

        when(productRepository.findById(200L)).thenReturn(Optional.of(owner));
        when(productRelatedRepository.findAllByProduct(owner, PageRequest.of(0, 10))).thenReturn(page);
        when(mediaService.getMedia(1001L)).thenReturn(new NoFileMediaVm(1001L, "", "", "", "https://p1"));

        ProductsGetVm result = productService.getRelatedProductsStorefront(200L, 0, 10);

        assertThat(result.productContent()).hasSize(1);
        assertThat(result.productContent().getFirst().id()).isEqualTo(100L);
        assertThat(result.productContent().getFirst().thumbnailUrl()).isEqualTo("https://p1");
    }

    @Test
    void getProductByIds_shouldMapAllProducts() {
        Product first = Product.builder().id(1L).name("A").slug("a").price(10D).build();
        Product second = Product.builder().id(2L).name("B").slug("b").price(20D).build();

        when(productRepository.findAllByIdIn(List.of(1L, 2L))).thenReturn(List.of(first, second));

        List<ProductListVm> result = productService.getProductByIds(List.of(1L, 2L));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(1).id()).isEqualTo(2L);
    }

    @Test
    void getProductsWithFilter_shouldMapPagedProducts() {
        Product product = Product.builder().id(51L).name("Phone").slug("phone").price(100D).build();
        Page<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 5), 1);

        when(productRepository.getProductsWithFilter("phone", "Brand", PageRequest.of(0, 5))).thenReturn(page);

        var result = productService.getProductsWithFilter(0, 5, " Phone ", "Brand ");

        assertThat(result.productContent()).hasSize(1);
        assertThat(result.productContent().getFirst().id()).isEqualTo(51L);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void getProductById_shouldMapBrandCategoryImagesAndParent() {
        Brand brand = new Brand();
        brand.setId(8L);

        Category category = new Category();
        category.setId(9L);
        ProductCategory productCategory = ProductCategory.builder().category(category).build();

        Product parent = Product.builder().id(60L).build();
        Product product = Product.builder()
            .id(61L)
            .name("Detail")
            .slug("detail")
            .thumbnailMediaId(700L)
            .productImages(List.of(ProductImage.builder().imageId(701L).build()))
            .productCategories(List.of(productCategory))
            .brand(brand)
            .parent(parent)
            .build();

        when(productRepository.findById(61L)).thenReturn(Optional.of(product));
        when(mediaService.getMedia(700L)).thenReturn(new NoFileMediaVm(700L, "", "", "", "https://thumb"));
        when(mediaService.getMedia(701L)).thenReturn(new NoFileMediaVm(701L, "", "", "", "https://img"));

        ProductDetailVm result = productService.getProductById(61L);

        assertThat(result.id()).isEqualTo(61L);
        assertThat(result.brandId()).isEqualTo(8L);
        assertThat(result.categories()).hasSize(1);
        assertThat(result.thumbnailMedia().url()).isEqualTo("https://thumb");
        assertThat(result.productImageMedias()).hasSize(1);
        assertThat(result.parentId()).isEqualTo(60L);
    }

    @Test
    void getProductsFromCategory_shouldReturnPagedProducts() {
        Category category = new Category();
        category.setId(88L);
        category.setSlug("electronics");

        Product product = Product.builder().id(90L).name("TV").slug("tv").thumbnailMediaId(901L).build();
        ProductCategory pc = ProductCategory.builder().category(category).product(product).build();
        Page<ProductCategory> page = new PageImpl<>(List.of(pc), PageRequest.of(0, 10), 1);

        when(categoryRepository.findBySlug("electronics")).thenReturn(Optional.of(category));
        when(productCategoryRepository.findAllByCategory(PageRequest.of(0, 10), category)).thenReturn(page);
        when(mediaService.getMedia(901L)).thenReturn(new NoFileMediaVm(901L, "", "", "", "https://tv"));

        var result = productService.getProductsFromCategory(0, 10, "electronics");

        assertThat(result.productContent()).hasSize(1);
        assertThat(result.productContent().getFirst().id()).isEqualTo(90L);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void getListFeaturedProducts_shouldMapResult() {
        Product product = Product.builder().id(100L).name("Featured").slug("featured").price(150D)
            .thumbnailMediaId(1000L).build();
        Page<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 5), 1);

        when(productRepository.getFeaturedProduct(PageRequest.of(0, 5))).thenReturn(page);
        when(mediaService.getMedia(1000L)).thenReturn(new NoFileMediaVm(1000L, "", "", "", "https://featured"));

        ProductFeatureGetVm result = productService.getListFeaturedProducts(0, 5);

        assertThat(result.productList()).hasSize(1);
        assertThat(result.productList().getFirst().id()).isEqualTo(100L);
        assertThat(result.totalPage()).isEqualTo(1);
    }

    @Test
    void getProductDetail_shouldMapAttributeGroupsAndImages() {
        ProductAttributeGroup group = new ProductAttributeGroup();
        group.setId(1L);
        group.setName("Specs");

        ProductAttribute colorAttr = ProductAttribute.builder().id(11L).name("Color").productAttributeGroup(group).build();
        ProductAttribute codeAttr = ProductAttribute.builder().id(12L).name("Code").productAttributeGroup(null).build();

        ProductAttributeValue colorValue = new ProductAttributeValue();
        colorValue.setProductAttribute(colorAttr);
        colorValue.setValue("Red");
        ProductAttributeValue codeValue = new ProductAttributeValue();
        codeValue.setProductAttribute(codeAttr);
        codeValue.setValue("X1");

        Category category = new Category();
        category.setName("Electronics");
        ProductCategory productCategory = ProductCategory.builder().category(category).build();

        Brand brand = new Brand();
        brand.setName("BrandY");

        Product product = Product.builder()
            .id(200L)
            .name("Detail Product")
            .slug("detail-product")
            .price(100D)
            .brand(brand)
            .productCategories(List.of(productCategory))
            .thumbnailMediaId(2001L)
            .productImages(List.of(ProductImage.builder().imageId(2002L).build()))
            .attributeValues(List.of(colorValue, codeValue))
            .build();

        when(productRepository.findBySlugAndIsPublishedTrue("detail-product")).thenReturn(Optional.of(product));
        when(mediaService.getMedia(2001L)).thenReturn(new NoFileMediaVm(2001L, "", "", "", "https://thumb"));
        when(mediaService.getMedia(2002L)).thenReturn(new NoFileMediaVm(2002L, "", "", "", "https://img"));

        ProductDetailGetVm result = productService.getProductDetail("detail-product");

        assertThat(result.id()).isEqualTo(200L);
        assertThat(result.brandName()).isEqualTo("BrandY");
        assertThat(result.productCategories()).containsExactly("Electronics");
        assertThat(result.productAttributeGroups()).hasSize(2);
        assertThat(result.thumbnailMediaUrl()).isEqualTo("https://thumb");
        assertThat(result.productImageMediaUrls()).containsExactly("https://img");
    }

    @Test
    void deleteProduct_whenMainProduct_shouldSetPublishedFalseOnly() {
        Product product = Product.builder().id(300L).isPublished(true).parent(null).build();
        when(productRepository.findById(300L)).thenReturn(Optional.of(product));

        productService.deleteProduct(300L);

        assertThat(product.isPublished()).isFalse();
        verify(productOptionCombinationRepository, never()).deleteAll(any());
        verify(productRepository).save(product);
    }

    @Test
    void deleteProduct_whenVariantAndHasCombinations_shouldDeleteCombinations() {
        Product parent = Product.builder().id(400L).build();
        Product variant = Product.builder().id(401L).parent(parent).isPublished(true).build();
        ProductOptionCombination combination = ProductOptionCombination.builder().id(1L).product(variant).build();

        when(productRepository.findById(401L)).thenReturn(Optional.of(variant));
        when(productOptionCombinationRepository.findAllByProduct(variant)).thenReturn(List.of(combination));

        productService.deleteProduct(401L);

        verify(productOptionCombinationRepository).deleteAll(List.of(combination));
        verify(productRepository).save(variant);
    }

    @Test
    void getProductEsDetailById_shouldMapBrandCategoriesAndAttributes() {
        Category category = new Category();
        category.setName("Laptop");
        ProductCategory productCategory = ProductCategory.builder().category(category).build();

        ProductAttribute attribute = ProductAttribute.builder().name("RAM").build();
        ProductAttributeValue attributeValue = new ProductAttributeValue();
        attributeValue.setProductAttribute(attribute);

        Brand brand = new Brand();
        brand.setName("BrandZ");

        Product product = Product.builder()
            .id(500L)
            .name("ES Product")
            .slug("es-product")
            .price(300D)
            .isPublished(true)
            .isVisibleIndividually(true)
            .isAllowedToOrder(true)
            .isFeatured(false)
            .thumbnailMediaId(5001L)
            .brand(brand)
            .productCategories(List.of(productCategory))
            .attributeValues(List.of(attributeValue))
            .build();

        when(productRepository.findById(500L)).thenReturn(Optional.of(product));

        ProductEsDetailVm result = productService.getProductEsDetailById(500L);

        assertThat(result.id()).isEqualTo(500L);
        assertThat(result.brand()).isEqualTo("BrandZ");
        assertThat(result.categories()).containsExactly("Laptop");
        assertThat(result.attributes()).containsExactly("RAM");
    }

    @Test
    void getProductsForWarehouse_shouldMapProducts() {
        Product first = Product.builder().id(1L).name("A").sku("A-1").build();
        Product second = Product.builder().id(2L).name("B").sku("B-2").build();

        when(productRepository.findProductForWarehouse("a", "sku", List.of(1L, 2L), "ALL"))
            .thenReturn(List.of(first, second));

        List<ProductInfoVm> result = productService.getProductsForWarehouse("a", "sku", List.of(1L, 2L),
            FilterExistInWhSelection.ALL);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().id()).isEqualTo(1L);
        assertThat(result.get(1).sku()).isEqualTo("B-2");
    }

    @Test
    void createProduct_whenNoVariationOrOptionValue_shouldReturnSavedMainProduct() {
        ProductPostVm postVm = new ProductPostVm(
            "Main",
            "main",
            null,
            List.of(),
            "short",
            "description",
            "spec",
            "sku-1",
            "",
            1D,
            null,
            2D,
            1D,
            1D,
            10D,
            true,
            true,
            false,
            true,
            true,
            "meta",
            "keyword",
            "meta-desc",
            1L,
            List.of(10L),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            1L
        );

        Product saved = Product.builder().id(600L).name("Main").slug("main").productCategories(List.of()).build();
        when(productRepository.findBySlugAndIsPublishedTrue("main")).thenReturn(Optional.empty());
        when(productRepository.findBySkuAndIsPublishedTrue("sku-1")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductGetDetailVm result = productService.createProduct(postVm);

        assertThat(result.id()).isEqualTo(600L);
        assertThat(result.name()).isEqualTo("Main");
        verify(productImageRepository).saveAll(any());
        verify(productCategoryRepository).saveAll(any());
    }

    @Test
    void updateProduct_whenNoNewVariation_shouldUpdateAndReturn() {
        Product existing = Product.builder()
            .id(700L)
            .slug("existing")
            .sku("sku-old")
            .gtin("gtin-old")
            .name("Old")
            .productCategories(List.of())
            .relatedProducts(List.of())
            .products(List.of())
            .build();

        ProductPutVm putVm = new ProductPutVm(
            "Updated",
            "updated",
            20D,
            true,
            true,
            true,
            true,
            true,
            null,
            List.of(),
            "short",
            "desc",
            "spec",
            "sku-new",
            "",
            1D,
            null,
            2D,
            1D,
            1D,
            "meta",
            "meta-key",
            "meta-desc",
            8L,
            List.of(),
            List.of(),
            List.of(new ProductOptionValuePutVm(1L, "dropdown", 1, List.of("v1"))),
            List.of(),
            List.of(),
            2L
        );

        ProductOption option = new ProductOption();
        option.setId(1L);

        when(productRepository.findById(700L)).thenReturn(Optional.of(existing));
        when(productRepository.findBySlugAndIsPublishedTrue("updated")).thenReturn(Optional.empty());
        when(productRepository.findBySkuAndIsPublishedTrue("sku-new")).thenReturn(Optional.empty());
        when(productCategoryRepository.findAllByProductId(700L)).thenReturn(List.of());
        when(productOptionRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(option));
        when(productOptionValueRepository.saveAll(any())).thenReturn(List.of());

        productService.updateProduct(700L, putVm);

        assertThat(existing.getName()).isEqualTo("Updated");
        assertThat(existing.getSlug()).isEqualTo("updated");
        verify(productRepository).saveAll(existing.getProducts());
        verify(productImageRepository).saveAll(any());
    }

    @Test
    void createProduct_whenLengthLessThanWidth_shouldThrowBadRequest() {
        ProductPostVm postVm = new ProductPostVm(
            "Main", "main", null, List.of(), "s", "d", "spec", "sku", "", 1D, null,
            1D, 2D, 1D, 10D, true, true, false, true, true,
            "meta", "keyword", "meta-desc", 1L, List.of(), List.of(), List.of(), List.of(), List.of(), 1L
        );

        assertThatThrownBy(() -> productService.createProduct(postVm))
            .isInstanceOf(com.yas.commonlibrary.exception.BadRequestException.class);
    }

    @Test
    void updateProduct_whenProductNotFound_shouldThrowNotFoundException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        ProductPutVm putVm = new ProductPutVm(
            "Name", "slug", 1D, true, true, false, true, true,
            null, List.of(), "s", "d", "spec", "sku", "", 1D, null,
            2D, 1D, 1D, "m", "k", "md", 1L,
            List.of(), List.of(), List.of(new ProductOptionValuePutVm(1L, "dropdown", 1, List.of("v"))),
            List.of(), List.of(), 1L
        );

        assertThatThrownBy(() -> productService.updateProduct(999L, putVm))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getProductById_whenNotFound_shouldThrowNotFoundException() {
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(404L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getProductDetail_whenNotFound_shouldThrowNotFoundException() {
        when(productRepository.findBySlugAndIsPublishedTrue("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductDetail("missing"))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getProductsFromCategory_whenCategoryNotFound_shouldThrowNotFoundException() {
        when(categoryRepository.findBySlug("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductsFromCategory(0, 5, "missing"))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteProduct_whenNotFound_shouldThrowNotFoundException() {
        when(productRepository.findById(1234L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(1234L))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getProductSlug_whenNotFound_shouldThrowNotFoundException() {
        when(productRepository.findById(2222L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductSlug(2222L))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getProductVariationsByParentId_whenParentNotFound_shouldThrowNotFoundException() {
        when(productRepository.findById(3333L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductVariationsByParentId(3333L))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getProductEsDetailById_whenNotFound_shouldThrowNotFoundException() {
        when(productRepository.findById(4444L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductEsDetailById(4444L))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getRelatedProductsStorefront_whenProductNotFound_shouldThrowNotFoundException() {
        when(productRepository.findById(5555L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getRelatedProductsStorefront(5555L, 0, 5))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getFeaturedProductsById_whenThumbnailPresent_shouldUseOwnThumbnail() {
        Product product = Product.builder()
            .id(123L)
            .name("P")
            .slug("p")
            .price(9D)
            .thumbnailMediaId(321L)
            .build();

        when(productRepository.findAllByIdIn(List.of(123L))).thenReturn(List.of(product));
        when(mediaService.getMedia(321L)).thenReturn(new NoFileMediaVm(321L, "", "", "", "https://own"));

        List<ProductThumbnailGetVm> result = productService.getFeaturedProductsById(List.of(123L));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().thumbnailUrl()).isEqualTo("https://own");
    }

    @Test
    void createProduct_whenVariationsAndOptionsValid_shouldCreateCombinations() {
        ProductOption option = new ProductOption();
        option.setId(1L);

        ProductVariationPostVm variation = new ProductVariationPostVm(
            "Variant Red",
            "variant-red",
            "SKU-V1",
            "GTIN-V1",
            12D,
            1200L,
            List.of(1201L),
            Map.of(1L, "Red")
        );

        ProductPostVm postVm = new ProductPostVm(
            "Main Product",
            "main-product",
            null,
            List.of(),
            "short",
            "description",
            "spec",
            "SKU-MAIN",
            "GTIN-MAIN",
            1D,
            DimensionUnit.CM,
            2D,
            1D,
            1D,
            20D,
            true,
            true,
            false,
            true,
            true,
            "meta",
            "keyword",
            "meta-desc",
            1100L,
            List.of(1101L),
            List.of(variation),
            List.of(new ProductOptionValuePostVm(1L, "dropdown", 1, List.of("Red"))),
            List.of(ProductOptionValueDisplay.builder().productOptionId(1L).displayType("dropdown")
                .displayOrder(1).value("Red").build()),
            List.of(),
            1L
        );

        when(productRepository.findBySlugAndIsPublishedTrue(any())).thenReturn(Optional.empty());
        when(productRepository.findBySkuAndIsPublishedTrue(any())).thenReturn(Optional.empty());
        when(productRepository.findByGtinAndIsPublishedTrue(any())).thenReturn(Optional.empty());
        when(productRepository.findAllById(List.of())).thenReturn(List.of());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            if (product.getId() == null) {
                product.setId(600L);
            }
            return product;
        });
        when(productRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(productOptionRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(option));
        when(productOptionValueRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProductGetDetailVm result = productService.createProduct(postVm);

        assertThat(result.slug()).isEqualTo("main-product");
        verify(productRepository).saveAll(any());
        verify(productOptionValueRepository).saveAll(any());
        verify(productOptionCombinationRepository).saveAll(any());
    }

    @Test
    void createProduct_whenVariationOptionValueMissing_shouldThrowBadRequest() {
        ProductOption option = new ProductOption();
        option.setId(1L);

        ProductVariationPostVm variation = new ProductVariationPostVm(
            "Variant",
            "variant",
            "SKU-V",
            "",
            10D,
            2000L,
            List.of(),
            Map.of(2L, "Blue")
        );

        ProductPostVm postVm = new ProductPostVm(
            "Main",
            "main",
            null,
            List.of(),
            "short",
            "description",
            "spec",
            "SKU-M",
            "",
            1D,
            null,
            2D,
            1D,
            1D,
            10D,
            true,
            true,
            false,
            true,
            true,
            "meta",
            "k",
            "meta-desc",
            1L,
            List.of(),
            List.of(variation),
            List.of(new ProductOptionValuePostVm(1L, "dropdown", 1, List.of("Red"))),
            List.of(ProductOptionValueDisplay.builder().productOptionId(1L).displayType("dropdown")
                .displayOrder(1).value("Red").build()),
            List.of(),
            1L
        );

        when(productRepository.findBySlugAndIsPublishedTrue(any())).thenReturn(Optional.empty());
        when(productRepository.findBySkuAndIsPublishedTrue(any())).thenReturn(Optional.empty());
        when(productRepository.findAllById(List.of())).thenReturn(List.of());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(productOptionRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(option));
        when(productOptionValueRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> productService.createProduct(postVm)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void createProduct_whenSavedVariationSlugMismatch_shouldThrowInternalServerError() {
        ProductOption option = new ProductOption();
        option.setId(1L);

        ProductVariationPostVm variation = new ProductVariationPostVm(
            "Variant",
            "variant-one",
            "SKU-V1",
            "",
            10D,
            2000L,
            List.of(),
            Map.of(1L, "Blue")
        );

        ProductPostVm postVm = new ProductPostVm(
            "Main",
            "main",
            null,
            List.of(),
            "short",
            "description",
            "spec",
            "SKU-M",
            "",
            1D,
            null,
            2D,
            1D,
            1D,
            10D,
            true,
            true,
            false,
            true,
            true,
            "meta",
            "k",
            "meta-desc",
            1L,
            List.of(),
            List.of(variation),
            List.of(new ProductOptionValuePostVm(1L, "dropdown", 1, List.of("Blue"))),
            List.of(ProductOptionValueDisplay.builder().productOptionId(1L).displayType("dropdown")
                .displayOrder(1).value("Blue").build()),
            List.of(),
            1L
        );

        Product wrongVariation = Product.builder().slug("another-slug").build();

        when(productRepository.findBySlugAndIsPublishedTrue(any())).thenReturn(Optional.empty());
        when(productRepository.findBySkuAndIsPublishedTrue(any())).thenReturn(Optional.empty());
        when(productRepository.findAllById(List.of())).thenReturn(List.of());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.saveAll(any())).thenReturn(List.of(wrongVariation));
        when(productOptionRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(option));
        when(productOptionValueRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> productService.createProduct(postVm))
            .isInstanceOf(InternalServerErrorException.class);
    }

    @Test
    void updateProduct_whenHasNewVariation_shouldCreateVariationAndCombinations() {
        ProductOption option = new ProductOption();
        option.setId(1L);

        Product existingVariation = Product.builder().id(801L).productImages(List.of()).build();
        Product existing = Product.builder()
            .id(800L)
            .slug("old")
            .sku("sku-old")
            .gtin("gtin-old")
            .name("Old")
            .productCategories(List.of())
            .relatedProducts(List.of())
            .products(List.of(existingVariation))
            .build();

        ProductVariationPutVm existingVm = new ProductVariationPutVm(
            801L, "Existing V", "existing-v", "SKU-E", "", 11D, 1801L, List.of(1802L), Map.of(1L, "Red")
        );
        ProductVariationPutVm newVm = new ProductVariationPutVm(
            null, "New V", "new-v", "SKU-N", "", 12D, 1803L, List.of(1804L), Map.of(1L, "Blue")
        );

        ProductPutVm putVm = new ProductPutVm(
            "Updated",
            "updated",
            25D,
            true,
            true,
            true,
            true,
            true,
            null,
            List.of(),
            "short",
            "desc",
            "spec",
            "sku-new",
            "",
            1D,
            null,
            2D,
            1D,
            1D,
            "meta",
            "meta-key",
            "meta-desc",
            8L,
            List.of(),
            List.of(existingVm, newVm),
            List.of(new ProductOptionValuePutVm(1L, "dropdown", 1, List.of("Blue"))),
            List.of(ProductOptionValueDisplay.builder().productOptionId(1L).displayType("dropdown")
                .displayOrder(1).value("Blue").build()),
            List.of(),
            2L
        );

        Product savedNewVariation = Product.builder().slug("new-v").id(802L).build();

        when(productRepository.findById(800L)).thenReturn(Optional.of(existing));
        when(productRepository.findBySlugAndIsPublishedTrue(any())).thenReturn(Optional.empty());
        when(productRepository.findBySkuAndIsPublishedTrue(any())).thenReturn(Optional.empty());
        when(productRepository.findAllById(any())).thenReturn(List.of());
        when(productCategoryRepository.findAllByProductId(800L)).thenReturn(List.of());
        when(productOptionRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(option));
        when(productOptionValueRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.saveAll(any())).thenReturn(List.of(savedNewVariation));

        productService.updateProduct(800L, putVm);

        verify(productOptionCombinationRepository).saveAll(any());
        verify(productRepository).save(existing);
        assertThat(existing.isHasOptions()).isTrue();
    }
}
