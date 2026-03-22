package com.yas.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.inventory.model.Warehouse;
import com.yas.inventory.model.enumeration.FilterExistInWhSelection;
import com.yas.inventory.repository.StockRepository;
import com.yas.inventory.repository.WarehouseRepository;
import com.yas.inventory.viewmodel.address.AddressDetailVm;
import com.yas.inventory.viewmodel.address.AddressPostVm;
import com.yas.inventory.viewmodel.address.AddressVm;
import com.yas.inventory.viewmodel.product.ProductInfoVm;
import com.yas.inventory.viewmodel.warehouse.WarehouseDetailVm;
import com.yas.inventory.viewmodel.warehouse.WarehouseListGetVm;
import com.yas.inventory.viewmodel.warehouse.WarehousePostVm;
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

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private StockRepository stockRepository;
    @Mock
    private ProductService productService;
    @Mock
    private LocationService locationService;

    private WarehouseService warehouseService;

    @BeforeEach
    void setUp() {
        warehouseService = new WarehouseService(warehouseRepository, stockRepository, productService, locationService);
    }

    @Test
    void findAllWarehouses_shouldReturnMappedData() {
        when(warehouseRepository.findAll()).thenReturn(List.of(
            Warehouse.builder().id(1L).name("WH-1").addressId(10L).build(),
            Warehouse.builder().id(2L).name("WH-2").addressId(20L).build()
        ));

        var result = warehouseService.findAllWarehouses();

        assertEquals(2, result.size());
        assertEquals("WH-1", result.get(0).name());
        assertEquals("WH-2", result.get(1).name());
    }

    @Test
    void getProductWarehouse_whenWarehouseHasProducts_shouldSetExistsFlag() {
        Long warehouseId = 11L;
        List<Long> productIds = List.of(1L, 3L);
        when(stockRepository.getProductIdsInWarehouse(warehouseId)).thenReturn(productIds);
        when(productService.filterProducts("shirt", "sku", productIds, FilterExistInWhSelection.YES))
            .thenReturn(List.of(
                new ProductInfoVm(1L, "P1", "S1", false),
                new ProductInfoVm(2L, "P2", "S2", false)
            ));

        var result = warehouseService.getProductWarehouse(warehouseId, "shirt", "sku", FilterExistInWhSelection.YES);

        assertEquals(2, result.size());
        assertTrue(result.get(0).existInWh());
        assertFalse(result.get(1).existInWh());
    }

    @Test
    void getProductWarehouse_whenWarehouseEmpty_shouldReturnProductServiceResult() {
        Long warehouseId = 11L;
        when(stockRepository.getProductIdsInWarehouse(warehouseId)).thenReturn(List.of());
        List<ProductInfoVm> filtered = List.of(new ProductInfoVm(2L, "P2", "S2", false));
        when(productService.filterProducts("shirt", "sku", List.of(), FilterExistInWhSelection.NO)).thenReturn(filtered);

        var result = warehouseService.getProductWarehouse(warehouseId, "shirt", "sku", FilterExistInWhSelection.NO);

        assertEquals(filtered, result);
    }

    @Test
    void findById_whenMissing_shouldThrowNotFound() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> warehouseService.findById(1L));
    }

    @Test
    void findById_whenFound_shouldReturnWarehouseDetail() {
        Warehouse warehouse = Warehouse.builder().id(7L).name("Main").addressId(88L).build();
        when(warehouseRepository.findById(7L)).thenReturn(Optional.of(warehouse));
        when(locationService.getAddressById(88L)).thenReturn(AddressDetailVm.builder()
            .contactName("John")
            .phone("123")
            .addressLine1("a1")
            .addressLine2("a2")
            .city("city")
            .zipCode("zip")
            .districtId(1L)
            .stateOrProvinceId(2L)
            .countryId(3L)
            .build());

        WarehouseDetailVm vm = warehouseService.findById(7L);

        assertEquals(7L, vm.id());
        assertEquals("Main", vm.name());
        assertEquals("John", vm.contactName());
    }

    @Test
    void create_whenNameExists_shouldThrowDuplicated() {
        WarehousePostVm request = buildWarehousePostVm();
        when(warehouseRepository.existsByName(request.name())).thenReturn(true);

        assertThrows(DuplicatedException.class, () -> warehouseService.create(request));
        verify(locationService, never()).createAddress(any(AddressPostVm.class));
    }

    @Test
    void create_whenValid_shouldSaveWarehouse() {
        WarehousePostVm request = buildWarehousePostVm();
        when(warehouseRepository.existsByName(request.name())).thenReturn(false);
        when(locationService.createAddress(any(AddressPostVm.class))).thenReturn(AddressVm.builder().id(55L).build());
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Warehouse result = warehouseService.create(request);

        assertEquals("warehouse-a", result.getName());
        assertEquals(55L, result.getAddressId());
    }

    @Test
    void update_whenMissing_shouldThrowNotFound() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> warehouseService.update(buildWarehousePostVm(), 1L));
    }

    @Test
    void update_whenDuplicateName_shouldThrowDuplicated() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(Warehouse.builder().id(1L).addressId(10L).build()));
        when(warehouseRepository.existsByNameWithDifferentId("warehouse-a", 1L)).thenReturn(true);

        assertThrows(DuplicatedException.class, () -> warehouseService.update(buildWarehousePostVm(), 1L));
        verify(locationService, never()).updateAddress(any(), any());
    }

    @Test
    void update_whenValid_shouldUpdateWarehouseAndAddress() {
        Warehouse warehouse = Warehouse.builder().id(1L).name("old").addressId(10L).build();
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(warehouseRepository.existsByNameWithDifferentId("warehouse-a", 1L)).thenReturn(false);

        warehouseService.update(buildWarehousePostVm(), 1L);

        assertEquals("warehouse-a", warehouse.getName());
        verify(locationService).updateAddress(any(Long.class), any(AddressPostVm.class));
        verify(warehouseRepository).save(warehouse);
    }

    @Test
    void delete_whenFound_shouldDeleteWarehouseAndAddress() {
        Warehouse warehouse = Warehouse.builder().id(5L).name("w").addressId(101L).build();
        when(warehouseRepository.findById(5L)).thenReturn(Optional.of(warehouse));

        warehouseService.delete(5L);

        verify(warehouseRepository).deleteById(5L);
        verify(locationService).deleteAddress(101L);
    }

    @Test
    void getPageableWarehouses_shouldReturnListVm() {
        List<Warehouse> data = List.of(
            Warehouse.builder().id(1L).name("W1").addressId(1L).build(),
            Warehouse.builder().id(2L).name("W2").addressId(2L).build());
        Page<Warehouse> page = new PageImpl<>(data, PageRequest.of(0, 2), 5);
        when(warehouseRepository.findAll(PageRequest.of(0, 2))).thenReturn(page);

        WarehouseListGetVm vm = warehouseService.getPageableWarehouses(0, 2);

        assertEquals(2, vm.warehouseContent().size());
        assertEquals(5, vm.totalElements());
        assertFalse(vm.isLast());
    }

    private WarehousePostVm buildWarehousePostVm() {
        return WarehousePostVm.builder()
            .name("warehouse-a")
            .contactName("contact")
            .phone("123")
            .addressLine1("line1")
            .addressLine2("line2")
            .city("city")
            .zipCode("zip")
            .districtId(1L)
            .stateOrProvinceId(2L)
            .countryId(3L)
            .build();
    }
}
