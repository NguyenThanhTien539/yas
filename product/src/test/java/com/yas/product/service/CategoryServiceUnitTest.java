package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Category;
import com.yas.product.repository.CategoryRepository;
import com.yas.product.viewmodel.NoFileMediaVm;
import com.yas.product.viewmodel.category.CategoryPostVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class CategoryServiceUnitTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private MediaService mediaService;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void create_whenParentExists_shouldSaveCategoryWithParent() {
        Category parent = new Category();
        parent.setId(10L);
        CategoryPostVm postVm = new CategoryPostVm("Category", "category", "desc", 10L,
            "keywords", "meta", (short) 1, true, 100L);

        when(categoryRepository.findExistedName("Category", null)).thenReturn(null);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Category result = categoryService.create(postVm);

        assertThat(result.getParent()).isEqualTo(parent);
        assertThat(result.getSlug()).isEqualTo("category");
        assertThat(result.getImageId()).isEqualTo(100L);
    }

    @Test
    void create_whenParentNotFound_shouldThrowBadRequestException() {
        CategoryPostVm postVm = new CategoryPostVm("Category", "category", "desc", 10L,
            "keywords", "meta", (short) 1, true, 100L);

        when(categoryRepository.findExistedName("Category", null)).thenReturn(null);
        when(categoryRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.create(postVm)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_whenNameDuplicated_shouldThrowDuplicatedException() {
        CategoryPostVm postVm = new CategoryPostVm("Category", "category", "desc", null,
            "keywords", "meta", (short) 1, true, 100L);

        when(categoryRepository.findExistedName("Category", null)).thenReturn(new Category());

        assertThatThrownBy(() -> categoryService.create(postVm)).isInstanceOf(DuplicatedException.class);
    }

    @Test
    void update_whenParentIsSelf_shouldThrowBadRequestException() {
        Category existing = new Category();
        existing.setId(5L);
        CategoryPostVm postVm = new CategoryPostVm("Updated", "updated", "desc", 5L,
            "keywords", "meta", (short) 1, true, 100L);

        when(categoryRepository.findExistedName("Updated", 5L)).thenReturn(null);
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> categoryService.update(postVm, 5L)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void update_whenParentCleared_shouldRemoveParent() {
        Category existing = new Category();
        existing.setId(5L);
        Category oldParent = new Category();
        oldParent.setId(2L);
        existing.setParent(oldParent);
        CategoryPostVm postVm = new CategoryPostVm("Updated", "updated", "desc", null,
            "keywords", "meta", (short) 1, true, 100L);

        when(categoryRepository.findExistedName("Updated", 5L)).thenReturn(null);
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(existing));

        categoryService.update(postVm, 5L);

        assertThat(existing.getParent()).isNull();
        assertThat(existing.getName()).isEqualTo("Updated");
    }

    @Test
    void getCategoryById_whenNoImageAndNoParent_shouldReturnDefaults() {
        Category category = new Category();
        category.setId(7L);
        category.setName("Name");
        category.setSlug("slug");
        category.setDisplayOrder((short) 1);
        category.setIsPublished(true);

        when(categoryRepository.findById(7L)).thenReturn(Optional.of(category));

        var result = categoryService.getCategoryById(7L);

        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.parentId()).isZero();
        assertThat(result.categoryImage()).isNull();
    }

    @Test
    void getPageableCategories_shouldMapPageMetadata() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Name");
        category.setSlug("slug");
        var page = new PageImpl<>(List.of(category), PageRequest.of(0, 1), 1);

        when(categoryRepository.findAll(PageRequest.of(0, 1))).thenReturn(page);

        var result = categoryService.getPageableCategories(0, 1);

        assertThat(result.categoryContent()).hasSize(1);
        assertThat(result.pageNo()).isZero();
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void getCategoryByIds_shouldMapAllCategories() {
        Category first = new Category();
        first.setId(1L);
        first.setName("A");
        first.setSlug("a");
        Category second = new Category();
        second.setId(2L);
        second.setName("B");
        second.setSlug("b");

        when(categoryRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(first, second));

        var result = categoryService.getCategoryByIds(List.of(1L, 2L));

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().id()).isEqualTo(1L);
    }

    @Test
    void getTopNthCategories_shouldDelegateToRepository() {
        when(categoryRepository.findCategoriesOrderedByProductCount(PageRequest.of(0, 3)))
            .thenReturn(List.of("C1", "C2", "C3"));

        var result = categoryService.getTopNthCategories(3);

        assertThat(result).containsExactly("C1", "C2", "C3");
    }

    @Test
    void getCategories_whenImageIsNull_shouldMapWithoutMediaLookup() {
        Category category = new Category();
        category.setId(1L);
        category.setName("A");
        category.setSlug("a");

        when(categoryRepository.findByNameContainingIgnoreCase("A")).thenReturn(List.of(category));

        var result = categoryService.getCategories("A");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().categoryImage()).isNull();
        verify(mediaService, never()).getMedia(any());
    }
}
