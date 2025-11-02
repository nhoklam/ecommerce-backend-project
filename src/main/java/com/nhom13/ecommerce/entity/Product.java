package com.nhom13.ecommerce.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
// [MỚI] Imports
import com.nhom13.ecommerce.util.JpaMapConverter;
import java.util.Map;
// import java.util.Set; // Đã xóa import không sử dụng

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Product extends BaseEntity {
    
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity")
    private Integer stockQuantity = 0;

    @Column(unique = true, length = 100)
    private String sku;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_active") 
    private Boolean isActive = true;

    // [MỚI] Thêm cờ Nổi bật
    @Column(name = "is_featured")
    private Boolean isFeatured = false;

    // [MỚI] Thêm Thông số kỹ thuật
    @Column(columnDefinition = "TEXT")
    @Convert(converter = JpaMapConverter.class)
    private Map<String, String> specifications;

    // [MỚI] Thêm Đánh giá
    // Xóa 'precision' và 'scale' khỏi kiểu 'Double'
    @Column(name = "average_rating")
    private Double averageRating = 0.0;

    @Column(name = "review_count")
    private Integer reviewCount = 0;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CartItem> cartItems;
    
    // [MỚI] Liên kết đến Reviews
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Review> reviews;
}