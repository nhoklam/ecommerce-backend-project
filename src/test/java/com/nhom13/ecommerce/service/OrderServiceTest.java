package com.nhom13.ecommerce.service;

import com.nhom13.ecommerce.dto.OrderDTO;
import com.nhom13.ecommerce.entity.*;
import com.nhom13.ecommerce.exception.BadRequestException;
import com.nhom13.ecommerce.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductService productService; // Rất quan trọng để mock logic kho
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private Address address;
    private Product product;
    private ProductVariant variant;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        address = new Address();
        address.setId(1L);
        address.setUser(user);
        address.setStreet("123 Test St");
        address.setWard("Phường 1");
        address.setDistrict("Quận 1");
        address.setCity("TP. Hồ Chí Minh");

        product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setPrice(new BigDecimal("100"));

        variant = new ProductVariant();
        variant.setId(1L);
        variant.setProduct(product);
        variant.setStockQuantity(10); // Có 10 sản phẩm
        variant.setSku("TEST-SKU");

        cartItem = new CartItem();
        cartItem.setId(1L);
        cartItem.setUser(user);
        cartItem.setProductVariant(variant);
        cartItem.setQuantity(2); // Mua 2 sản phẩm
    }

    @Test
    void createOrder_ShouldSucceed_WhenCartIsNotEmptyAndStockIsSufficient() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(address));
        when(cartItemRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(cartItem));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        // When
        OrderDTO result = orderService.createOrder(1L, 1L, "COD");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalAmount()).isEqualTo(new BigDecimal("200")); // 100 * 2
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.getPaymentStatus()).isEqualTo("PENDING");

        // Xác minh logic quan trọng:
        // 1. Kho đã bị trừ 
        verify(productService).updateVariantStock(variant.getId(), 2);
        // 2. Giỏ hàng đã bị xóa 
        verify(cartItemRepository).deleteAllByUserId(1L);
        // 3. Đơn hàng đã được lưu
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrder_ShouldThrowException_WhenCartIsEmpty() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(address));
        when(cartItemRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of()); // Giỏ hàng rỗng

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(1L, 1L, "COD"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cart is empty");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_ShouldThrowException_WhenInsufficientStock() {
        // Given
        cartItem.setQuantity(20); // Mua 20 (chỉ có 10 trong kho)
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(address));
        when(cartItemRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(cartItem));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(1L, 1L, "COD"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Insufficient stock for product: Test Product");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void cancelOrder_ShouldSucceed_AndRestoreStock_WhenOrderIsPending() {
        // Given
        Order order = new Order();
        order.setId(1L);
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setOrderItems(List.of(new OrderItem(1L, order, variant, 2, new BigDecimal("100"), new BigDecimal("200"), 0)));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When
        orderService.cancelOrder(1L, 1L);

        // Then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        // Xác minh logic quan trọng: Kho đã được hoàn lại 
        verify(productService).restoreVariantStock(variant.getId(), 2);
        verify(orderRepository).save(order);
    }

    @Test
    void cancelOrder_ShouldThrowException_WhenOrderIsShipped() {
        // Given
        Order order = new Order();
        order.setId(1L);
        order.setUser(user);
        order.setStatus(OrderStatus.SHIPPED); // Đã giao hàng

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Order cannot be cancelled");

        verify(productService, never()).restoreVariantStock(anyLong(), anyInt());
    }

    @Test
    void handleVnPayCallback_ShouldUpdateStatus_AndRestoreStock_WhenPaymentFails() {
        // Given
        Order order = new Order();
        order.setId(1L);
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus("PENDING");
        order.setOrderItems(List.of(new OrderItem(1L, order, variant, 2, new BigDecimal("100"), new BigDecimal("200"), 0)));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // When
        // "24" là mã lỗi (ví dụ: Hủy thanh toán)
        OrderDTO result = orderService.handleVnPayCallback(1L, "24"); 

        // Then
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(result.getPaymentStatus()).isEqualTo("FAILED");
        // Xác minh logic quan trọng: Kho đã được hoàn lại 
        verify(productService).restoreVariantStock(variant.getId(), 2);
        verify(orderRepository).save(order);
    }

    @Test
    void handleVnPayCallback_ShouldUpdateStatus_WhenPaymentSucceeds() {
        // Given
        Order order = new Order();
        order.setId(1L);
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus("PENDING");
        order.setOrderItems(List.of(new OrderItem(1L, order, variant, 2, new BigDecimal("100"), new BigDecimal("200"), 0)));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // When
        // "00" là mã thành công
        OrderDTO result = orderService.handleVnPayCallback(1L, "00");

        // Then
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(result.getPaymentStatus()).isEqualTo("PAID");
        // Xác minh: KHÔNG hoàn kho
        verify(productService, never()).restoreVariantStock(anyLong(), anyInt());
        // Xác minh: KHÔNG trừ kho (vì đã trừ lúc tạo đơn) [cite: 316, 317, 318]
        verify(productService, never()).updateVariantStock(anyLong(), anyInt());
        verify(orderRepository).save(order);
    }
}