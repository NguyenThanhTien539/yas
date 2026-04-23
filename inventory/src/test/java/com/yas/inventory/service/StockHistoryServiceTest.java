package com.yas.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.inventory.model.Stock;
import com.yas.inventory.model.StockHistory;
import com.yas.inventory.model.Warehouse;
import com.yas.inventory.repository.StockHistoryRepository;
import com.yas.inventory.viewmodel.product.ProductInfoVm;
import com.yas.inventory.viewmodel.stock.StockQuantityVm;
import com.yas.inventory.viewmodel.stockhistory.StockHistoryListVm;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockHistoryServiceTest {

    @Mock
    private StockHistoryRepository stockHistoryRepository;
    @Mock
    private ProductService productService;

    private StockHistoryService stockHistoryService;

    @BeforeEach
    void setUp() {
        stockHistoryService = new StockHistoryService(stockHistoryRepository, productService);
    }

    @Test
    void createStockHistories_shouldSaveMatchedStocksOnly() {
        Warehouse warehouse = Warehouse.builder().id(2L).name("WH").build();
        Stock matchedStock = Stock.builder().id(1L).productId(10L).warehouse(warehouse).build();
        Stock unmatchedStock = Stock.builder().id(99L).productId(20L).warehouse(warehouse).build();
        StockQuantityVm quantityVm = new StockQuantityVm(1L, 5L, "new stock");

        stockHistoryService.createStockHistories(List.of(matchedStock, unmatchedStock), List.of(quantityVm));

        ArgumentCaptor<List<StockHistory>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockHistoryRepository).saveAll(captor.capture());
        List<StockHistory> saved = captor.getValue();
        assertEquals(1, saved.size());
        assertEquals(10L, saved.getFirst().getProductId());
        assertEquals(5L, saved.getFirst().getAdjustedQuantity());
        assertEquals("new stock", saved.getFirst().getNote());
    }

    @Test
    void getStockHistories_shouldReturnMappedVm() {
        Warehouse warehouse = Warehouse.builder().id(2L).name("WH").build();
        StockHistory stockHistory = StockHistory.builder()
            .id(7L)
            .productId(10L)
            .adjustedQuantity(9L)
            .note("note")
            .warehouse(warehouse)
            .build();
        when(stockHistoryRepository.findByProductIdAndWarehouseIdOrderByCreatedOnDesc(10L, 2L))
            .thenReturn(List.of(stockHistory));
        when(productService.getProduct(10L)).thenReturn(new ProductInfoVm(10L, "Product", "SKU", true));

        StockHistoryListVm result = stockHistoryService.getStockHistories(10L, 2L);

        assertEquals(1, result.data().size());
        assertEquals(7L, result.data().getFirst().id());
        assertEquals("Product", result.data().getFirst().productName());
        assertEquals(9L, result.data().getFirst().adjustedQuantity());
    }
}
