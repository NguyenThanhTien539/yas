package com.yas.tax.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.tax.model.TaxClass;
import com.yas.tax.model.TaxRate;
import com.yas.tax.repository.TaxClassRepository;
import com.yas.tax.repository.TaxRateRepository;
import com.yas.tax.viewmodel.location.StateOrProvinceAndCountryGetNameVm;
import com.yas.tax.viewmodel.taxrate.TaxRateListGetVm;
import com.yas.tax.viewmodel.taxrate.TaxRatePostVm;
import com.yas.tax.viewmodel.taxrate.TaxRateVm;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class TaxRateServiceTest {

    @Mock
    private TaxRateRepository taxRateRepository;
    @Mock
    private TaxClassRepository taxClassRepository;
    @Mock
    private LocationService locationService;

    private TaxRateService taxRateService;

    @BeforeEach
    void setUp() {
        taxRateService = new TaxRateService(locationService, taxRateRepository, taxClassRepository);
    }

    @Test
    void createTaxRate_whenTaxClassMissing_shouldThrowNotFound() {
        TaxRatePostVm request = new TaxRatePostVm(10.0, "70000", 1L, 2L, 3L);
        when(taxClassRepository.existsById(1L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> taxRateService.createTaxRate(request));
    }

    @Test
    void createTaxRate_whenValid_shouldSave() {
        TaxRatePostVm request = new TaxRatePostVm(10.0, "70000", 1L, 2L, 3L);
        TaxClass taxClass = TaxClass.builder().id(1L).name("VAT").build();
        when(taxClassRepository.existsById(1L)).thenReturn(true);
        when(taxClassRepository.getReferenceById(1L)).thenReturn(taxClass);
        when(taxRateRepository.save(any(TaxRate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaxRate saved = taxRateService.createTaxRate(request);

        assertEquals(10.0, saved.getRate());
        assertEquals("70000", saved.getZipCode());
        assertEquals(1L, saved.getTaxClass().getId());
    }

    @Test
    void updateTaxRate_whenRateMissing_shouldThrowNotFound() {
        when(taxRateRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
            () -> taxRateService.updateTaxRate(new TaxRatePostVm(5.0, null, 1L, null, 2L), 5L));
    }

    @Test
    void updateTaxRate_whenTaxClassMissing_shouldThrowNotFound() {
        when(taxRateRepository.findById(5L)).thenReturn(Optional.of(TaxRate.builder().id(5L).build()));
        when(taxClassRepository.existsById(1L)).thenReturn(false);

        assertThrows(NotFoundException.class,
            () -> taxRateService.updateTaxRate(new TaxRatePostVm(5.0, null, 1L, null, 2L), 5L));
    }

    @Test
    void updateTaxRate_whenValid_shouldUpdateAndSave() {
        TaxClass existingClass = TaxClass.builder().id(2L).name("Old").build();
        TaxClass newClass = TaxClass.builder().id(1L).name("VAT").build();
        TaxRate rate = TaxRate.builder()
            .id(5L)
            .rate(2.0)
            .zipCode("old")
            .taxClass(existingClass)
            .stateOrProvinceId(8L)
            .countryId(9L)
            .build();
        when(taxRateRepository.findById(5L)).thenReturn(Optional.of(rate));
        when(taxClassRepository.existsById(1L)).thenReturn(true);
        when(taxClassRepository.getReferenceById(1L)).thenReturn(newClass);

        taxRateService.updateTaxRate(new TaxRatePostVm(7.5, "70000", 1L, 3L, 4L), 5L);

        assertEquals(7.5, rate.getRate());
        assertEquals("70000", rate.getZipCode());
        assertEquals(1L, rate.getTaxClass().getId());
        assertEquals(3L, rate.getStateOrProvinceId());
        assertEquals(4L, rate.getCountryId());
        verify(taxRateRepository).save(rate);
    }

    @Test
    void delete_whenNotFound_shouldThrow() {
        when(taxRateRepository.existsById(10L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> taxRateService.delete(10L));
    }

    @Test
    void delete_whenFound_shouldDelete() {
        when(taxRateRepository.existsById(10L)).thenReturn(true);

        taxRateService.delete(10L);

        verify(taxRateRepository).deleteById(10L);
    }

    @Test
    void findById_whenFound_shouldMapVm() {
        TaxClass taxClass = TaxClass.builder().id(11L).name("VAT").build();
        TaxRate rate = TaxRate.builder().id(1L).rate(10.0).zipCode("70000").taxClass(taxClass).countryId(3L).build();
        when(taxRateRepository.findById(1L)).thenReturn(Optional.of(rate));

        TaxRateVm vm = taxRateService.findById(1L);

        assertEquals(1L, vm.id());
        assertEquals(11L, vm.taxClassId());
    }

    @Test
    void findAll_shouldReturnMappedList() {
        TaxClass taxClass = TaxClass.builder().id(11L).name("VAT").build();
        when(taxRateRepository.findAll()).thenReturn(List.of(
            TaxRate.builder().id(1L).rate(10.0).zipCode("70000").taxClass(taxClass).countryId(3L).build()
        ));

        List<TaxRateVm> result = taxRateService.findAll();

        assertEquals(1, result.size());
        assertEquals(1L, result.getFirst().id());
    }

    @Test
    void getPageableTaxRates_whenNoStateIds_shouldReturnEmptyContent() {
        Page<TaxRate> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(taxRateRepository.findAll(PageRequest.of(0, 10))).thenReturn(page);

        TaxRateListGetVm vm = taxRateService.getPageableTaxRates(0, 10);

        assertEquals(0, vm.taxRateGetDetailContent().size());
        assertEquals(0, vm.totalElements());
    }

    @Test
    void getPageableTaxRates_whenLocationReturned_shouldBuildDetailRows() {
        TaxClass taxClass = TaxClass.builder().id(11L).name("VAT").build();
        TaxRate rate = TaxRate.builder()
            .id(1L)
            .rate(10.0)
            .zipCode("70000")
            .taxClass(taxClass)
            .stateOrProvinceId(5L)
            .countryId(3L)
            .build();
        Page<TaxRate> page = new PageImpl<>(List.of(rate), PageRequest.of(0, 10), 1);
        when(taxRateRepository.findAll(PageRequest.of(0, 10))).thenReturn(page);
        when(locationService.getStateOrProvinceAndCountryNames(List.of(5L))).thenReturn(List.of(
            new StateOrProvinceAndCountryGetNameVm(5L, "Ho Chi Minh", "Vietnam")
        ));

        TaxRateListGetVm vm = taxRateService.getPageableTaxRates(0, 10);

        assertEquals(1, vm.taxRateGetDetailContent().size());
        assertEquals("VAT", vm.taxRateGetDetailContent().getFirst().taxClassName());
        assertEquals("Ho Chi Minh", vm.taxRateGetDetailContent().getFirst().stateOrProvinceName());
    }

    @Test
    void getTaxPercent_whenValueExists_shouldReturnValue() {
        when(taxRateRepository.getTaxPercent(1L, 2L, "70000", 3L)).thenReturn(9.25);

        double result = taxRateService.getTaxPercent(3L, 1L, 2L, "70000");

        assertEquals(9.25, result);
    }

    @Test
    void getTaxPercent_whenNull_shouldReturnZero() {
        when(taxRateRepository.getTaxPercent(1L, 2L, "70000", 3L)).thenReturn(null);

        double result = taxRateService.getTaxPercent(3L, 1L, 2L, "70000");

        assertEquals(0.0, result);
    }

    @Test
    void getBulkTaxRate_shouldMapResult() {
        TaxClass taxClass = TaxClass.builder().id(4L).name("VAT").build();
        TaxRate entity = TaxRate.builder()
            .id(15L)
            .rate(8.0)
            .zipCode("70000")
            .taxClass(taxClass)
            .stateOrProvinceId(5L)
            .countryId(3L)
            .build();
        when(taxRateRepository.getBatchTaxRates(3L, 5L, "70000", Set.of(4L, 7L)))
            .thenReturn(List.of(entity));

        List<TaxRateVm> result = taxRateService.getBulkTaxRate(List.of(4L, 7L), 3L, 5L, "70000");

        assertEquals(1, result.size());
        assertEquals(15L, result.getFirst().id());
        assertEquals(4L, result.getFirst().taxClassId());
    }
}
