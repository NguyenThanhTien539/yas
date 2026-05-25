package com.yas.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.commonlibrary.exception.StockExistingException;
import com.yas.inventory.model.Stock;
import com.yas.inventory.model.Warehouse;
import com.yas.inventory.model.enumeration.FilterExistInWhSelection;
import com.yas.inventory.repository.StockRepository;
import com.yas.inventory.repository.WarehouseRepository;
import com.yas.inventory.viewmodel.product.ProductInfoVm;
import com.yas.inventory.viewmodel.product.ProductQuantityPostVm;
import com.yas.inventory.viewmodel.stock.StockPostVm;
import com.yas.inventory.viewmodel.stock.StockQuantityUpdateVm;
import com.yas.inventory.viewmodel.stock.StockQuantityVm;
import com.yas.inventory.viewmodel.stock.StockVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private StockRepository stockRepository;
    @Mock
    private ProductService productService;
    @Mock
    private WarehouseService warehouseService;
    @Mock
    private StockHistoryService stockHistoryService;

    private StockService stockService;

    @BeforeEach
    void setUp() {
        stockService = new StockService(
            warehouseRepository,
            stockRepository,
            productService,
            warehouseService,
            stockHistoryService
        );
    }

    @Test
    void addProductIntoWarehouse_whenStockAlreadyExists_shouldThrow() {
        StockPostVm input = new StockPostVm(10L, 2L);
        when(stockRepository.existsByWarehouseIdAndProductId(2L, 10L)).thenReturn(true);

        assertThrows(StockExistingException.class, () -> stockService.addProductIntoWarehouse(List.of(input)));
        verify(stockRepository, never()).saveAll(any());
    }

    @Test
    void addProductIntoWarehouse_whenProductMissing_shouldThrow() {
        StockPostVm input = new StockPostVm(10L, 2L);
        when(stockRepository.existsByWarehouseIdAndProductId(2L, 10L)).thenReturn(false);
        when(productService.getProduct(10L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> stockService.addProductIntoWarehouse(List.of(input)));
    }

    @Test
    void addProductIntoWarehouse_whenWarehouseMissing_shouldThrow() {
        StockPostVm input = new StockPostVm(10L, 2L);
        when(stockRepository.existsByWarehouseIdAndProductId(2L, 10L)).thenReturn(false);
        when(productService.getProduct(10L)).thenReturn(new ProductInfoVm(10L, "P", "S", true));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> stockService.addProductIntoWarehouse(List.of(input)));
    }

    @Test
    void addProductIntoWarehouse_whenValid_shouldCreateZeroQuantityStock() {
        StockPostVm input = new StockPostVm(10L, 2L);
        Warehouse warehouse = Warehouse.builder().id(2L).name("WH").build();
        when(stockRepository.existsByWarehouseIdAndProductId(2L, 10L)).thenReturn(false);
        when(productService.getProduct(10L)).thenReturn(new ProductInfoVm(10L, "P", "S", true));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouse));

        stockService.addProductIntoWarehouse(List.of(input));

        ArgumentCaptor<List<Stock>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockRepository).saveAll(captor.capture());
        Stock saved = captor.getValue().getFirst();
        assertEquals(10L, saved.getProductId());
        assertEquals(0L, saved.getQuantity());
        assertEquals(0L, saved.getReservedQuantity());
        assertEquals(2L, saved.getWarehouse().getId());
    }

    @Test
    void getStocksByWarehouseIdAndProductNameAndSku_shouldMapStockVm() {
        Long warehouseId = 1L;
        when(warehouseService.getProductWarehouse(warehouseId, "name", "sku", FilterExistInWhSelection.YES))
            .thenReturn(List.of(new ProductInfoVm(10L, "Shirt", "SKU-1", true)));
        Warehouse warehouse = Warehouse.builder().id(warehouseId).name("W").build();
        Stock stock = Stock.builder().id(100L).productId(10L).quantity(8L).reservedQuantity(2L).warehouse(warehouse).build();
        when(stockRepository.findByWarehouseIdAndProductIdIn(warehouseId, List.of(10L))).thenReturn(List.of(stock));

        List<StockVm> result = stockService.getStocksByWarehouseIdAndProductNameAndSku(warehouseId, "name", "sku");

        assertEquals(1, result.size());
        assertEquals("Shirt", result.getFirst().productName());
        assertEquals("SKU-1", result.getFirst().productSku());
        assertEquals(8L, result.getFirst().quantity());
    }

    @Test
    void updateProductQuantityInStock_whenValid_shouldPersistAndPropagate() {
        Warehouse warehouse = Warehouse.builder().id(2L).name("WH").build();
        Stock stock = Stock.builder().id(1L).productId(10L).quantity(5L).reservedQuantity(0L).warehouse(warehouse).build();
        StockQuantityVm quantityVm = new StockQuantityVm(1L, 3L, "add");
        StockQuantityUpdateVm request = new StockQuantityUpdateVm(List.of(quantityVm));

        when(stockRepository.findAllById(List.of(1L))).thenReturn(List.of(stock));

        stockService.updateProductQuantityInStock(request);

        assertEquals(8L, stock.getQuantity());
        verify(stockRepository).saveAll(List.of(stock));
        verify(stockHistoryService).createStockHistories(List.of(stock), List.of(quantityVm));

        ArgumentCaptor<List<ProductQuantityPostVm>> productQuantityCaptor = ArgumentCaptor.forClass(List.class);
        verify(productService).updateProductQuantity(productQuantityCaptor.capture());
        assertEquals(1, productQuantityCaptor.getValue().size());
        assertEquals(10L, productQuantityCaptor.getValue().getFirst().productId());
        assertEquals(8L, productQuantityCaptor.getValue().getFirst().stockQuantity());
    }

    @Test
    void updateProductQuantityInStock_whenNoMatchedStock_shouldNotPropagateProductQuantity() {
        StockQuantityVm quantityVm = new StockQuantityVm(99L, 3L, "add");
        when(stockRepository.findAllById(List.of(99L))).thenReturn(List.of());

        stockService.updateProductQuantityInStock(new StockQuantityUpdateVm(List.of(quantityVm)));

        verify(stockRepository).saveAll(List.of());
        verify(stockHistoryService).createStockHistories(List.of(), List.of(quantityVm));
        verify(productService, never()).updateProductQuantity(any());
    }
}
