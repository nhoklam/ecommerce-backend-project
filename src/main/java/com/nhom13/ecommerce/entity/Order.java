package com.nhom13.ecommerce.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Order extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;
    
    // [MỚI] Thay thế bằng liên kết đến Address
    @ManyToOne(fetch = FetchType.EAGER) // Eager để lấy thông tin địa chỉ khi load Order
    // SỬA LỖI: Thay đổi 'nullable = false' thành 'nullable = true'
    @JoinColumn(name = "address_id", nullable = true)
    private Address shippingAddress;
    
    // [MỚI] Lưu lại một bản sao dạng String của địa chỉ tại thời điểm đặt hàng
    @Column(name = "shipping_address_snapshot", nullable = false, columnDefinition = "TEXT")
    private String shippingAddressSnapshot;
    
    // [MỚI]
    @Column(name = "payment_method")
    private String paymentMethod;
    // (Ví dụ: "COD", "STRIPE", "VNPAY")

    @Column(name = "payment_status")
    private String paymentStatus = "PENDING";
    // (Ví dụ: "PENDING", "PAID", "FAILED")

    @Column(name = "tracking_number")
    private String trackingNumber;
    // Số vận đơn

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems;
}