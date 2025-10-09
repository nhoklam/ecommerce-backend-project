package com.nhom13.ecommerce.controller;

import com.nhom13.ecommerce.dto.OrderDTO;
import com.nhom13.ecommerce.dto.UserDTO;
import com.nhom13.ecommerce.entity.OrderStatus;
import com.nhom13.ecommerce.service.OrderService;
import com.nhom13.ecommerce.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrderController {
    
    private final OrderService orderService;
    private final UserService userService;
    
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(
            @RequestParam String shippingAddress,
            Authentication authentication) {
        
        UserDTO user = userService.getUserByEmail(authentication.getName());
        OrderDTO order = orderService.createOrder(user.getId(), shippingAddress);
        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }
    
    @GetMapping
    public ResponseEntity<List<OrderDTO>> getMyOrders(Authentication authentication) {
        UserDTO user = userService.getUserByEmail(authentication.getName());
        List<OrderDTO> orders = orderService.getOrdersByUser(user.getId());
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/paginated")
    public ResponseEntity<Page<OrderDTO>> getMyOrdersPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        
        UserDTO user = userService.getUserByEmail(authentication.getName());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<OrderDTO> orders = orderService.getOrdersByUser(user.getId(), pageable);
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long orderId, Authentication authentication) {
        OrderDTO order = orderService.getOrderById(orderId);
        UserDTO user = userService.getUserByEmail(authentication.getName());
        
        // Check if order belongs to user (unless admin)
        if (!order.getUserId().equals(user.getId()) && !user.getRole().name().equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(order);
    }
    
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long orderId, Authentication authentication) {
        UserDTO user = userService.getUserByEmail(authentication.getName());
        orderService.cancelOrder(orderId, user.getId());
        return ResponseEntity.noContent().build();
    }
    
    // Admin endpoints
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        List<OrderDTO> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }
    
    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status) {
        
        OrderDTO updatedOrder = orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok(updatedOrder);
    }
}