package com.nhom13.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.nhom13.ecommerce.entity.OrderStatus;

import lombok.Data;

@Data
public class OrderDTO {
    private Long id;
    private Long userId;
    private UserDTO user;
    private BigDecimal totalAmount;
    private OrderStatus status;
    
    // [CŨ] private String shippingAddress;
    // [MỚI]
    private AddressDTO shippingAddress; // Dùng DTO
    private String shippingAddressSnapshot; // Giữ lại chuỗi snapshot

    private LocalDateTime createdAt;
    private List<OrderItemDTO> orderItems;

    // [MỚI]
    private String paymentMethod;
    private String paymentStatus;
    private String trackingNumber;
}