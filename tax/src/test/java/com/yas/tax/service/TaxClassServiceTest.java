package com.yas.tax.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.tax.model.TaxClass;
import com.yas.tax.repository.TaxClassRepository;
import com.yas.tax.viewmodel.taxclass.TaxClassListGetVm;
import com.yas.tax.viewmodel.taxclass.TaxClassPostVm;
import com.yas.tax.viewmodel.taxclass.TaxClassVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class TaxClassServiceTest {

    @Mock
    private TaxClassRepository taxClassRepository;

    private TaxClassService taxClassService;

    @BeforeEach
    void setUp() {
        taxClassService = new TaxClassService(taxClassRepository);
    }

    @Test
    void findAllTaxClasses_shouldReturnSortedMappedData() {
        when(taxClassRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))).thenReturn(List.of(
            TaxClass.builder().id(1L).name("A").build(),
            TaxClass.builder().id(2L).name("B").build()
        ));

        List<TaxClassVm> result = taxClassService.findAllTaxClasses();

        assertEquals(2, result.size());
        assertEquals("A", result.get(0).name());
    }

    @Test
    void findById_whenMissing_shouldThrowNotFound() {
        when(taxClassRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> taxClassService.findById(9L));
    }

    @Test
    void findById_whenFound_shouldReturnVm() {
        when(taxClassRepository.findById(9L)).thenReturn(Optional.of(TaxClass.builder().id(9L).name("VAT").build()));

        TaxClassVm vm = taxClassService.findById(9L);

        assertEquals(9L, vm.id());
        assertEquals("VAT", vm.name());
    }

    @Test
    void create_whenDuplicateName_shouldThrowDuplicated() {
        TaxClassPostVm request = new TaxClassPostVm("1", "VAT");
        when(taxClassRepository.existsByName("VAT")).thenReturn(true);

        assertThrows(DuplicatedException.class, () -> taxClassService.create(request));
    }

    @Test
    void create_whenValid_shouldSave() {
        TaxClassPostVm request = new TaxClassPostVm("1", "VAT");
        when(taxClassRepository.existsByName("VAT")).thenReturn(false);
        when(taxClassRepository.save(org.mockito.ArgumentMatchers.any(TaxClass.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        TaxClass result = taxClassService.create(request);

        assertEquals("VAT", result.getName());
    }

    @Test
    void update_whenNotFound_shouldThrowNotFound() {
        when(taxClassRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> taxClassService.update(new TaxClassPostVm("1", "VAT"), 1L));
    }

    @Test
    void update_whenDuplicateName_shouldThrowDuplicated() {
        when(taxClassRepository.findById(1L)).thenReturn(Optional.of(TaxClass.builder().id(1L).name("Old").build()));
        when(taxClassRepository.existsByNameNotUpdatingTaxClass("VAT", 1L)).thenReturn(true);

        assertThrows(DuplicatedException.class, () -> taxClassService.update(new TaxClassPostVm("1", "VAT"), 1L));
    }

    @Test
    void update_whenValid_shouldSave() {
        TaxClass existing = TaxClass.builder().id(1L).name("Old").build();
        when(taxClassRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taxClassRepository.existsByNameNotUpdatingTaxClass("VAT", 1L)).thenReturn(false);

        taxClassService.update(new TaxClassPostVm("1", "VAT"), 1L);

        assertEquals("VAT", existing.getName());
        verify(taxClassRepository).save(existing);
    }

    @Test
    void delete_whenNotFound_shouldThrowNotFound() {
        when(taxClassRepository.existsById(2L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> taxClassService.delete(2L));
    }

    @Test
    void delete_whenFound_shouldDelete() {
        when(taxClassRepository.existsById(2L)).thenReturn(true);

        taxClassService.delete(2L);

        verify(taxClassRepository).deleteById(2L);
    }

    @Test
    void getPageableTaxClasses_shouldReturnListVm() {
        Page<TaxClass> page = new PageImpl<>(
            List.of(TaxClass.builder().id(1L).name("VAT").build()),
            PageRequest.of(0, 10),
            1
        );
        when(taxClassRepository.findAll(PageRequest.of(0, 10))).thenReturn(page);

        TaxClassListGetVm vm = taxClassService.getPageableTaxClasses(0, 10);

        assertEquals(1, vm.taxClassContent().size());
        assertEquals("VAT", vm.taxClassContent().getFirst().name());
        assertEquals(1, vm.totalElements());
    }
}
