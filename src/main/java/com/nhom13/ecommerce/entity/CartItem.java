package com.nhom13.ecommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

// [MỚI] Thêm import cho ProductVariant
import com.nhom13.ecommerce.entity.ProductVariant;

@Entity
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
    @JoinColumn(name = "user_id", nullable = true) 
    private User user;
    
    // [XÓA] Đã loại bỏ mối quan hệ trực tiếp với Product
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "product_id", nullable = false)
    // private Product product;

    // [THAY THẾ] Một CartItem bây giờ sẽ trỏ đến một ProductVariant cụ thể
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;
    
    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "tempCartId")
    private String tempCartId;
}