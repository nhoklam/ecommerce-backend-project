package com.nhom13.ecommerce.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nhom13.ecommerce.dto.StatisticsDTO;
import com.nhom13.ecommerce.entity.OrderStatus;
import com.nhom13.ecommerce.repository.OrderRepository;
import com.nhom13.ecommerce.repository.ProductRepository;
import com.nhom13.ecommerce.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {
    
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    
    public StatisticsDTO getStatistics() {
        StatisticsDTO stats = new StatisticsDTO();
        
        // Basic counts
        stats.setTotalUsers(userRepository.count());
        stats.setTotalProducts(productRepository.count());
        stats.setTotalOrders(orderRepository.count());
        
        // Revenue calculation
        BigDecimal totalRevenue = orderRepository.findAll().stream()
            .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
            .map(order -> order.getTotalAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalRevenue(totalRevenue);
        
        // Orders by status
        Map<OrderStatus, Long> ordersByStatus = new HashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            long count = orderRepository.findByStatus(status).size();
            ordersByStatus.put(status, count);
        }
        stats.setOrdersByStatus(ordersByStatus);
        
        return stats;
    }
    
    public StatisticsDTO getMonthlyStatistics(int year, int month) {
        LocalDateTime startDate = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime endDate = startDate.plusMonths(1).minusSeconds(1);
        
        StatisticsDTO stats = new StatisticsDTO();
        
        var monthlyOrders = orderRepository.findOrdersByDateRange(startDate, endDate);
        stats.setTotalOrders((long) monthlyOrders.size());
        
        BigDecimal monthlyRevenue = monthlyOrders.stream()
            .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
            .map(order -> order.getTotalAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalRevenue(monthlyRevenue);
        
        return stats;
    }
}