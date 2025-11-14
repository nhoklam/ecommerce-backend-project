package com.nhom13.ecommerce.service;

import com.nhom13.ecommerce.dto.AddressDTO;
import com.nhom13.ecommerce.dto.OrderDTO;
import com.nhom13.ecommerce.dto.OrderItemDTO;
import com.nhom13.ecommerce.dto.UserDTO;
import com.nhom13.ecommerce.entity.*;
import com.nhom13.ecommerce.exception.BadRequestException;
import com.nhom13.ecommerce.exception.ResourceNotFoundException;
import com.nhom13.ecommerce.repository.*;
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
    private final ReviewRepository reviewRepository;

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

        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(address);
        order.setShippingAddressSnapshot(address.getFullAddress());
        order.setPaymentMethod(paymentMethod);

        List<OrderItem> orderItems = cartItems.stream().map(cartItem -> {
            ProductVariant variant = cartItem.getProductVariant();
            Product product = variant.getProduct();

            // 1. KIỂM TRA TỒN KHO
            if (variant.getStockQuantity() < cartItem.getQuantity()) {
                throw new BadRequestException("Insufficient stock for product: " + product.getName());
            }
            
            // 2. [SỬA ĐỔI] TRỪ KHO NGAY LẬP TỨC
            // Trừ kho ngay tại đây để "khóa" sản phẩm
            productService.updateVariantStock(variant.getId(), cartItem.getQuantity());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductVariant(variant);
            orderItem.setQuantity(cartItem.getQuantity());

            BigDecimal unitPrice = variant.getPrice() != null ? variant.getPrice() : product.getPrice();
            orderItem.setUnitPrice(unitPrice);
            orderItem.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            return orderItem;
        }).collect(Collectors.toList());

        BigDecimal totalAmount = orderItems.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        order.setTotalAmount(totalAmount);
        order.setOrderItems(orderItems);

        if ("COD".equalsIgnoreCase(paymentMethod)) {
            order.setStatus(OrderStatus.PENDING);
            order.setPaymentStatus("PENDING");
            // [SỬA ĐỔI] Không cần lưu ở đây, lưu 1 lần ở cuối
        } else if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
            order.setStatus(OrderStatus.PENDING);
            order.setPaymentStatus("PENDING");
        } else {
            throw new BadRequestException("Unsupported payment method: " + paymentMethod);
        }

        // 3. [SỬA ĐỔI] Lưu đơn hàng
        Order savedOrder = orderRepository.save(order);
        
        // 4. [SỬA ĐỔI] XÓA GIỎ HÀNG (cho cả COD và VNPAY)
        cartItemRepository.deleteAllByUserId(userId);
        
        log.info("Order {} created ({}). Stock deducted, cart cleared.", savedOrder.getId(), paymentMethod);
        return convertToDTO(savedOrder);
    }

    @Transactional
    public OrderDTO confirmCodPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!"COD".equalsIgnoreCase(order.getPaymentMethod()) || order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Order cannot be confirmed for payment.");
        }
        
        // [SỬA ĐỔI] XÓA LOGIC TRỪ KHO (Đã làm ở createOrder)
        // order.getOrderItems().forEach(orderItem -> {
        //     productService.updateVariantStock(orderItem.getProductVariant().getId(), orderItem.getQuantity());
        // });
        
        order.setStatus(OrderStatus.PROCESSING);
        order.setPaymentStatus("PAID");
        Order saved = orderRepository.save(order);

        // [SỬA ĐỔI] XÓA LOGIC XÓA GIỎ HÀNG (Đã làm ở createOrder)
        // cartItemRepository.deleteAllByUserId(order.getUser().getId());
        
        // ... (email logic)
        return convertToDTO(saved);
    }

    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("Order does not belong to user");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Order cannot be cancelled. Current status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.CANCELLED);
        
        // [SỬA ĐỔI] HOÀN KHO KHI HỦY ĐƠN
        // Vì kho đã bị trừ lúc createOrder, nên PENDING (chưa thanh toán)
        // khi hủy cũng phải hoàn kho.
        log.info("User cancelled order {}. Restoring stock.", orderId);
        try {
             order.getOrderItems().forEach(orderItem -> {
                productService.restoreVariantStock(orderItem.getProductVariant().getId(), orderItem.getQuantity());
            });
        } catch (Exception e) {
             log.error("CRITICAL: Failed to restore stock for cancelled order {}: {}", orderId, e.getMessage());
        }

        orderRepository.save(order);
    }

    public OrderDTO updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);

        // Hoàn kho nếu hủy một đơn hàng đã được xử lý (đã trừ kho)
        if (newStatus == OrderStatus.CANCELLED && (oldStatus == OrderStatus.PROCESSING || oldStatus == OrderStatus.SHIPPED)) {
            log.info("Admin cancelled processed order {}. Restoring stock.", orderId);
            order.getOrderItems().forEach(orderItem -> {
                productService.restoreVariantStock(orderItem.getProductVariant().getId(), orderItem.getQuantity());
            });
        }
        Order updatedOrder = orderRepository.save(order);
        // ... (email logic)
        return convertToDTO(updatedOrder);
    }

    // ... (Các phương thức get, update tracking không thay đổi) ...
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

    public OrderDTO updateOrderTracking(Long orderId, String trackingNumber) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        order.setTrackingNumber(trackingNumber);
        if (order.getStatus() == OrderStatus.PROCESSING) {
            order.setStatus(OrderStatus.SHIPPED);
        }
        Order updatedOrder = orderRepository.save(order);
        // ... (email logic)
        return convertToDTO(updatedOrder);
    }

    public OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setUserId(order.getUser().getId());

        UserDTO userDTO = new UserDTO();
        userDTO.setId(order.getUser().getId());
        userDTO.setFirstName(order.getUser().getFirstName());
        userDTO.setLastName(order.getUser().getLastName());
        userDTO.setEmail(order.getUser().getEmail());
        userDTO.setRole(order.getUser().getRole());
        dto.setUser(userDTO);

        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setPaymentStatus(order.getPaymentStatus());
        dto.setTrackingNumber(order.getTrackingNumber());
        if (order.getShippingAddress() != null) {
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
        if (order.getOrderItems() != null) {
            List<OrderItemDTO> orderItemDTOs = order.getOrderItems().stream()
                    .map(orderItem -> convertOrderItemToDTO(orderItem, order.getUser().getId()))
                    .collect(Collectors.toList());
            dto.setOrderItems(orderItemDTOs);
        }
        return dto;
    }

    private OrderItemDTO convertOrderItemToDTO(OrderItem orderItem, Long userId) {
        OrderItemDTO dto = new OrderItemDTO();
        ProductVariant variant = orderItem.getProductVariant();
        Product product = variant.getProduct();

        dto.setId(orderItem.getId());
        dto.setProductId(product.getId());
        dto.setProductVariantId(variant.getId());
        dto.setProductName(product.getName() + " (" + variant.getColor() + " - " + variant.getProductSize() + ")");
        dto.setQuantity(orderItem.getQuantity());
        dto.setUnitPrice(orderItem.getUnitPrice());
        dto.setTotalPrice(orderItem.getTotalPrice());
        boolean reviewed = reviewRepository.existsByUserIdAndProductId(userId, product.getId());
        dto.setReviewed(reviewed);

        return dto;
    }

    // [SỬA ĐỔI] XÓA LOGIC TRỪ KHO/XÓA GIỎ HÀNG
    // Chỉ cập nhật trạng thái
    @Transactional
    public OrderDTO handleVnPayCallback(Long orderId, String vnpResponseCode) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        if (!"PENDING".equals(order.getPaymentStatus())) {
            throw new BadRequestException("Order is not in a valid state for payment");
        }

        if ("00".equals(vnpResponseCode)) { // 00 là code thành công của VNPAY
            order.setStatus(OrderStatus.PROCESSING);
            order.setPaymentStatus("PAID");
            
            // [SỬA ĐỔI] XÓA HẾT LOGIC TRỪ KHO VÀ XÓA GIỎ HÀNG
            
            orderRepository.save(order);
        } else {
            order.setStatus(OrderStatus.CANCELLED);
            order.setPaymentStatus("FAILED");
            
            // [SỬA ĐỔI] THÊM LOGIC HOÀN KHO KHI THANH TOÁN THẤT BẠI
             try {
                log.info("Restoring stock for failed payment on order {}", orderId);
                order.getOrderItems().forEach(item -> {
                    productService.restoreVariantStock(item.getProductVariant().getId(), item.getQuantity());
                });
            } catch (Exception e) {
                log.error("CRITICAL: Failed to restore stock for cancelled order {}: {}", orderId, e.getMessage());
            }
            
            orderRepository.save(order);
        }

        return convertToDTO(order);
    }
}