package com.nhom13.ecommerce.service;

import com.nhom13.ecommerce.dto.OrderDTO;
import com.nhom13.ecommerce.dto.OrderItemDTO;
import com.nhom13.ecommerce.dto.UserDTO;
import com.nhom13.ecommerce.entity.*;
import com.nhom13.ecommerce.exception.BadRequestException;
import com.nhom13.ecommerce.exception.ResourceNotFoundException;
import com.nhom13.ecommerce.repository.CartItemRepository;
import com.nhom13.ecommerce.repository.OrderRepository;
import com.nhom13.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

// [MỚI] Imports
import com.nhom13.ecommerce.repository.AddressRepository;
import com.nhom13.ecommerce.entity.Address;
import com.nhom13.ecommerce.dto.AddressDTO;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j // [MỚI]
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductService productService;

    // [MỚI] Injects
    private final AddressRepository addressRepository;
    private final EmailService emailService;

    // Chữ ký phương thức
    public OrderDTO createOrder(Long userId, Long addressId, String paymentMethod) {
        User user = userRepository.findById(userId)
           .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        // [MỚI] Lấy địa chỉ
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
          .orElseThrow(() -> new ResourceNotFoundException("Address not found or does not belong to user"));

        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }
        
        // Create order
        Order order = new Order();
        order.setUser(user);
        
        // [MỚI] Gán địa chỉ
        order.setShippingAddress(address);
        order.setShippingAddressSnapshot(address.getFullAddress()); // Lưu snapshot
        
        order.setStatus(OrderStatus.PENDING);
        
        // [MỚI] Gán phương thức thanh toán
        order.setPaymentMethod(paymentMethod);
        if ("COD".equalsIgnoreCase(paymentMethod)) {
            order.setPaymentStatus("PENDING"); // Sẽ thanh toán khi nhận hàng
        } else {
            order.setPaymentStatus("PENDING"); // Chờ thanh toán online
        }

        // Create order items
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = cartItems.stream().map(cartItem -> {
            Product product = cartItem.getProduct();
            
            // Check stock availability
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new BadRequestException("Insufficient stock for product: " + product.getName());
            }
            
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            
            return orderItem;
        }).collect(Collectors.toList());

        // Calculate total amount
        totalAmount = orderItems.stream()
           .map(OrderItem::getTotalPrice)
           .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(totalAmount);
        order.setOrderItems(orderItems);
        
        // Save order
        Order savedOrder = orderRepository.save(order);

        // Update product stock
        orderItems.forEach(orderItem -> {
            productService.updateStock(orderItem.getProduct().getId(), orderItem.getQuantity());
        });

        // Clear cart
        cartItemRepository.deleteAllByUserId(userId);
        
        OrderDTO orderDTO = convertToDTO(savedOrder);

        // [MỚI] Gửi email xác nhận (Sửa lỗi)
        try {
            emailService.sendOrderConfirmation(user.getEmail(), orderDTO);
        } catch (Exception e) {
            log.error("Failed to send order confirmation email for orderId: {}", savedOrder.getId(), e);
        }

        return orderDTO;
    }
    
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
           .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        return convertToDTO(order);
    }
    
    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
           .stream()
           .map(this::convertToDTO)
           .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Page<OrderDTO> getOrdersByUser(Long userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
           .map(this::convertToDTO);
    }
    
    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAll().stream()
           .map(this::convertToDTO)
           .collect(Collectors.toList());
    }
    
    public OrderDTO updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
           .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);
        
        // [MỚI] Gửi email thông báo cập nhật
        try {
            emailService.sendOrderStatusUpdate(order.getUser().getEmail(), convertToDTO(updatedOrder));
        } catch (Exception e) {
            log.error("Failed to send order status update email for orderId: {}", updatedOrder.getId(), e);
        }
        
        return convertToDTO(updatedOrder);
    }
    
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
           .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("Order does not belong to user");
        }
        
        if (order.getStatus()!= OrderStatus.PENDING) {
            throw new BadRequestException("Order cannot be cancelled. Current status: " + order.getStatus());
        }
        
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        // Restore product stock
        order.getOrderItems().forEach(orderItem -> {
            Product product = orderItem.getProduct();
            product.setStockQuantity(product.getStockQuantity() + orderItem.getQuantity());
        });
    }

    // [MỚI] Thêm phương thức cho Admin cập nhật tracking
    public OrderDTO updateOrderTracking(Long orderId, String trackingNumber) {
        Order order = orderRepository.findById(orderId)
          .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        order.setTrackingNumber(trackingNumber);
        // Tự động chuyển status sang SHIPPED khi có tracking
        if(order.getStatus() == OrderStatus.PROCESSING) {
            order.setStatus(OrderStatus.SHIPPED);
        }
        
        Order updatedOrder = orderRepository.save(order);
        
        // Gửi email thông báo
        try {
            emailService.sendOrderStatusUpdate(order.getUser().getEmail(), convertToDTO(updatedOrder));
        } catch (Exception e) {
            log.error("Failed to send order status update email for orderId: {}", updatedOrder.getId(), e);
        }
        
        return convertToDTO(updatedOrder);
    }
    
    private OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setUserId(order.getUser().getId());

        // Thêm thông tin user
        UserDTO userDTO = new UserDTO();
        userDTO.setId(order.getUser().getId());
        userDTO.setFirstName(order.getUser().getFirstName());
        userDTO.setLastName(order.getUser().getLastName());
        userDTO.setEmail(order.getUser().getEmail());
        userDTO.setRole(order.getUser().getRole());
        dto.setUser(userDTO);

        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus());
        
        // Convert Address entity sang AddressDTO
        if (order.getShippingAddress()!= null) {
            AddressDTO addressDTO = new AddressDTO();
            addressDTO.setId(order.getShippingAddress().getId());
            addressDTO.setFullName(order.getShippingAddress().getFullName());
            addressDTO.setPhone(order.getShippingAddress().getPhone());
            addressDTO.setStreet(order.getShippingAddress().getStreet());
            addressDTO.setCity(order.getShippingAddress().getCity());
            addressDTO.setDistrict(order.getShippingAddress().getDistrict());
            addressDTO.setWard(order.getShippingAddress().getWard());
            addressDTO.setIsDefault(order.getShippingAddress().getIsDefault());
            dto.setShippingAddress(addressDTO);
        }
        dto.setShippingAddressSnapshot(order.getShippingAddressSnapshot());
        
        dto.setCreatedAt(order.getCreatedAt());

        if (order.getOrderItems()!= null) {
            List<OrderItemDTO> orderItemDTOs = order.getOrderItems().stream()
               .map(this::convertOrderItemToDTO)
               .collect(Collectors.toList());
            dto.setOrderItems(orderItemDTOs);
        }

        // [MỚI] Map các trường mới
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setPaymentStatus(order.getPaymentStatus());
        dto.setTrackingNumber(order.getTrackingNumber());

        return dto;
    }
    
    private OrderItemDTO convertOrderItemToDTO(OrderItem orderItem) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(orderItem.getId());
        dto.setProductId(orderItem.getProduct().getId());
        dto.setProductName(orderItem.getProduct().getName());
        dto.setQuantity(orderItem.getQuantity());
        dto.setUnitPrice(orderItem.getUnitPrice());
        dto.setTotalPrice(orderItem.getTotalPrice());
        return dto;
    }
}