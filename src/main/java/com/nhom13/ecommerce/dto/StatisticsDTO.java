package com.nhom13.ecommerce.dto;

import com.nhom13.ecommerce.entity.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class StatisticsDTO {
    private Long totalUsers;
    private Long totalProducts;
    private Long totalOrders;
    private BigDecimal totalRevenue;
    private Map<OrderStatus, Long> ordersByStatus;
}