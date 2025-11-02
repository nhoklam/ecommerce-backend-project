package com.nhom13.ecommerce.service;

import com.nhom13.ecommerce.dto.AddressDTO;
import com.nhom13.ecommerce.dto.OrderDTO;
import com.nhom13.ecommerce.dto.OrderItemDTO;
import com.nhom13.ecommerce.dto.UserDTO;
import com.nhom13.ecommerce.entity.*;
import com.nhom13.ecommerce.exception.BadRequestException;
import com.nhom13.ecommerce.exception.ResourceNotFoundException;
import com.nhom13.ecommerce.repository.AddressRepository;
import com.nhom13.ecommerce.repository.CartItemRepository;
import com.nhom13.ecommerce.repository.OrderRepository;
import com.nhom13.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductService productService;
    private final AddressRepository addressRepository;
    private final EmailService emailService;

    @Transactional
    public OrderDTO createOrder(Long userId, Long addressId, String paymentMethod) {
        User user = userRepository.findById(userId)
         .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
        .orElseThrow(() -> new ResourceNotFoundException("Address not found or does not belong to user"));
        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }
        
        // Bước 1: Tạo Order và OrderItems (Logic chung cho mọi phương thức)
        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(address);
        order.setShippingAddressSnapshot(address.getFullAddress());
        order.setStatus(OrderStatus.PENDING); // Mọi đơn hàng đều bắt đầu là PENDING
        order.setPaymentMethod(paymentMethod);
        
        List<OrderItem> orderItems = cartItems.stream().map(cartItem -> {
            Product product = cartItem.getProduct();
            // Kiểm tra tồn kho
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

        BigDecimal totalAmount = orderItems.stream()
         .map(OrderItem::getTotalPrice)
         .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(totalAmount);
        order.setOrderItems(orderItems);

        // Bước 2: Xử lý Logic dựa trên Phương thức Thanh toán
        if ("COD".equalsIgnoreCase(paymentMethod)) {
            // Logic cũ (COD): Xử lý ngay lập tức
            order.setPaymentStatus("PENDING"); // Sẽ thanh toán khi nhận hàng
            order.setStatus(OrderStatus.PROCESSING); // Chuyển sang xử lý ngay
            
            // Lưu đơn hàng
            Order savedOrder = orderRepository.save(order);
            
            // Thực hiện tác dụng phụ ngay lập tức
            orderItems.forEach(orderItem -> {
                productService.updateStock(orderItem.getProduct().getId(), orderItem.getQuantity());
            });
            cartItemRepository.deleteAllByUserId(userId);
            
            // Gửi email xác nhận
            OrderDTO orderDTO = convertToDTO(savedOrder);
            try {
                emailService.sendOrderConfirmation(user.getEmail(), orderDTO);
            } catch (Exception e) {
                log.error("Failed to send order confirmation email for COD orderId: {}", savedOrder.getId(), e);
            }
            return orderDTO;

        } else if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
            // Logic mới (VNPAY): Chỉ tạo đơn hàng, chờ thanh toán
            order.setPaymentStatus("PENDING"); // Trạng thái PENDING quan trọng
            order.setStatus(OrderStatus.PENDING); // Chờ thanh toán
            
            // Chỉ lưu đơn hàng, KHÔNG thực hiện tác dụng phụ
            Order savedOrder = orderRepository.save(order);
            log.info("Pending order {} created for VNPAY payment.", savedOrder.getId());
            
            // Không trừ kho, không xóa giỏ hàng, không gửi email.
            // Các hành động này sẽ được `VnPayServiceImpl.handlePaymentCallback` kích hoạt.
            
            return convertToDTO(savedOrder);
        } else {
            throw new BadRequestException("Unsupported payment method: " + paymentMethod);
        }
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
        
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
        
        // Sửa lỗi cú pháp ||
        if (newStatus == OrderStatus.CANCELLED && 
            (oldStatus == OrderStatus.PROCESSING || oldStatus == OrderStatus.SHIPPED)) 
        {
            // Chỉ hoàn kho cho các đơn hàng đã bị trừ kho (ví dụ: COD, hoặc VNPAY đã thanh toán)
            log.info("Admin cancelled processed order {}. Restoring stock.", orderId);
            order.getOrderItems().forEach(orderItem -> {
                // Giả định ProductService có phương thức restoreStock
                // productService.restoreStock(orderItem.getProduct().getId(), orderItem.getQuantity());
                
                // Theo logic của cancelOrder cũ:
                Product product = orderItem.getProduct();
                product.setStockQuantity(product.getStockQuantity() + orderItem.getQuantity());
            });
        }

        Order updatedOrder = orderRepository.save(order);
        
        // Gửi email thông báo cập nhật
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
        
        // User có thể hủy đơn PENDING (VNPAY) hoặc PROCESSING (COD)
        if (order.getStatus()!= OrderStatus.PENDING && order.getStatus()!= OrderStatus.PROCESSING) {
            throw new BadRequestException("Order cannot be cancelled. Current status: " + order.getStatus());
        }
        
        boolean restoreStock = false;
        // Chỉ hoàn kho nếu là đơn COD (đã bị trừ kho khi tạo)
        if ("COD".equalsIgnoreCase(order.getPaymentMethod()) && order.getStatus() == OrderStatus.PROCESSING) {
            restoreStock = true;
        }
        // (Đơn VNPAY PENDING chưa bị trừ kho, nên không cần hoàn)

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        if (restoreStock) {
            log.info("Restoring stock for cancelled COD order {}", orderId);
            // Restore product stock
            order.getOrderItems().forEach(orderItem -> {
                Product product = orderItem.getProduct();
                product.setStockQuantity(product.getStockQuantity() + orderItem.getQuantity());
            });
        }
    }

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
    
    /**
     * Chuyển thành public để VnPayServiceImpl có thể sử dụng.
     */
    public OrderDTO convertToDTO(Order order) {
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