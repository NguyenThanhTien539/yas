package com.yas.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.order.mapper.OrderMapper;
import com.yas.order.model.Order;
import com.yas.order.model.OrderAddress;
import com.yas.order.model.enumeration.DeliveryMethod;
import com.yas.order.model.enumeration.DeliveryStatus;
import com.yas.order.model.enumeration.OrderStatus;
import com.yas.order.model.enumeration.PaymentStatus;
import com.yas.order.repository.OrderItemRepository;
import com.yas.order.repository.OrderRepository;
import com.yas.order.viewmodel.order.OrderBriefVm;
import com.yas.order.viewmodel.order.PaymentOrderStatusVm;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

/**
 * Partial tests for OrderService.
 * Happy-path + not-found cases covered for: getLatestOrders, getOrderWithItemsById,
 * findOrderByCheckoutId, rejectOrder, acceptOrder, updateOrderPaymentStatus.
 *
 * NOT covered (intentionally): createOrder, getAllOrder,
 * isOrderCompletedWithUserIdAndProductId, getMyOrders,
 * findOrderVmByCheckoutId, exportCsv
 * → overall line coverage stays below 70% → pipeline FAIL at coverage gate.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ProductService productService;
    @Mock private CartService cartService;
    @Mock private OrderMapper orderMapper;
    @Mock private PromotionService promotionService;

    @InjectMocks
    private OrderService orderService;

    // ─── Helper ───────────────────────────────────────────────────────────────

    private Order buildSampleOrder(long id) {
        OrderAddress address = OrderAddress.builder()
                .contactName("Test User")
                .phone("0123456789")
                .addressLine1("123 Test St")
                .city("Ho Chi Minh")
                .zipCode("70000")
                .build();

        return Order.builder()
                .id(id)
                .email("test@example.com")
                .orderStatus(OrderStatus.PENDING)
                .deliveryStatus(DeliveryStatus.PREPARING)
                .paymentStatus(PaymentStatus.PENDING)
                .deliveryMethod(DeliveryMethod.VIETTEL_POST)
                .totalPrice(BigDecimal.valueOf(100))
                .deliveryFee(BigDecimal.valueOf(10))
                .shippingAddressId(address)
                .billingAddressId(address)
                .build();
    }

    // ─── getLatestOrders ───────────────────────────────────────────────────────

    @Test
    void getLatestOrders_whenCountIsZero_shouldReturnEmptyList() {
        assertThat(orderService.getLatestOrders(0)).isEmpty();
    }

    @Test
    void getLatestOrders_whenCountIsNegative_shouldReturnEmptyList() {
        assertThat(orderService.getLatestOrders(-5)).isEmpty();
    }

    @Test
    void getLatestOrders_whenRepositoryReturnsEmpty_shouldReturnEmptyList() {
        when(orderRepository.getLatestOrders(any(Pageable.class))).thenReturn(Collections.emptyList());
        assertThat(orderService.getLatestOrders(5)).isEmpty();
    }

    @Test
    void getLatestOrders_whenOrdersExist_shouldReturnMappedList() {
        Order order = buildSampleOrder(1L);
        when(orderRepository.getLatestOrders(any(Pageable.class))).thenReturn(List.of(order));

        List<OrderBriefVm> result = orderService.getLatestOrders(3);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("test@example.com");
        assertThat(result.get(0).orderStatus()).isEqualTo(OrderStatus.PENDING);
    }

    // ─── getOrderWithItemsById ─────────────────────────────────────────────────

    @Test
    void getOrderWithItemsById_whenNotFound_shouldThrowNotFoundException() {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> orderService.getOrderWithItemsById(999L));
    }

    @Test
    void getOrderWithItemsById_whenFound_shouldReturnOrderVm() {
        Order order = buildSampleOrder(1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(1L)).thenReturn(Collections.emptyList());

        var result = orderService.getOrderWithItemsById(1L);

        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.PENDING);
    }

    // ─── findOrderByCheckoutId ─────────────────────────────────────────────────

    @Test
    void findOrderByCheckoutId_whenNotFound_shouldThrowNotFoundException() {
        when(orderRepository.findByCheckoutId(anyString())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,
                () -> orderService.findOrderByCheckoutId("non-existent-id"));
    }

    @Test
    void findOrderByCheckoutId_whenFound_shouldReturnOrder() {
        Order order = buildSampleOrder(2L);
        when(orderRepository.findByCheckoutId("chk-001")).thenReturn(Optional.of(order));

        Order result = orderService.findOrderByCheckoutId("chk-001");

        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    // ─── rejectOrder ──────────────────────────────────────────────────────────

    @Test
    void rejectOrder_whenNotFound_shouldThrowNotFoundException() {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> orderService.rejectOrder(999L, "out of stock"));
    }

    @Test
    void rejectOrder_whenFound_shouldSetStatusRejectAndSave() {
        Order order = buildSampleOrder(1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.rejectOrder(1L, "no stock");

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.REJECT);
        assertThat(order.getRejectReason()).isEqualTo("no stock");
        verify(orderRepository).save(order);
    }

    // ─── acceptOrder ──────────────────────────────────────────────────────────

    @Test
    void acceptOrder_whenNotFound_shouldThrowNotFoundException() {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> orderService.acceptOrder(999L));
    }

    @Test
    void acceptOrder_whenFound_shouldSetStatusAcceptedAndSave() {
        Order order = buildSampleOrder(1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.acceptOrder(1L);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.ACCEPTED);
        verify(orderRepository).save(order);
    }

    // ─── updateOrderPaymentStatus ─────────────────────────────────────────────

    @Test
    void updateOrderPaymentStatus_whenNotFound_shouldThrowNotFoundException() {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

        PaymentOrderStatusVm vm = PaymentOrderStatusVm.builder()
                .orderId(999L)
                .paymentId(1L)
                .paymentStatus(PaymentStatus.PENDING.name())
                .orderStatus(OrderStatus.PENDING.getName())
                .build();

        assertThrows(NotFoundException.class, () -> orderService.updateOrderPaymentStatus(vm));
    }

    @Test
    void updateOrderPaymentStatus_whenPending_shouldKeepOriginalOrderStatus() {
        Order order = buildSampleOrder(1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        PaymentOrderStatusVm vm = PaymentOrderStatusVm.builder()
                .orderId(1L)
                .paymentId(42L)
                .paymentStatus(PaymentStatus.PENDING.name())
                .orderStatus(OrderStatus.PENDING.getName())
                .build();

        PaymentOrderStatusVm result = orderService.updateOrderPaymentStatus(vm);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.paymentId()).isEqualTo(42L);
        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.PENDING.name());
    }

    @Test
    void updateOrderPaymentStatus_whenCompleted_shouldSetOrderStatusPaid() {
        Order order = buildSampleOrder(1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        PaymentOrderStatusVm vm = PaymentOrderStatusVm.builder()
                .orderId(1L)
                .paymentId(42L)
                .paymentStatus(PaymentStatus.COMPLETED.name())
                .orderStatus(OrderStatus.PENDING.getName())
                .build();

        orderService.updateOrderPaymentStatus(vm);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
    }
}
