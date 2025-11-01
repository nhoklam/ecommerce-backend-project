package com.nhom13.ecommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
// SỬA LỖI:
// 1. Xóa uniqueConstraints để Service xử lý logic, tránh lỗi DB với giá trị NULL.
// 2. Thêm Indexes để tăng tốc độ truy vấn cho cả user và guest.
@Table(name = "cart_items",
    indexes = {
        @Index(name = "idx_cartitem_user_id", columnList = "user_id"),
        @Index(name = "idx_cartitem_temp_cart_id", columnList = "tempCartId")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CartItem extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    // SỬA LỖI: Cho phép user_id là NULL (nullable = true) để hỗ trợ giỏ hàng của guest
    @JoinColumn(name = "user_id", nullable = true) 
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(nullable = false)
    private Integer quantity;

    // SỬA LỖI: Thêm trường tempCartId để lưu giỏ hàng của guest
    @Column(name = "tempCartId")
    private String tempCartId;
}
