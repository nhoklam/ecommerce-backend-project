package com.nhom13.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Entity
@Table(name = "product_variants")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductVariant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(length = 50)
    private String color; // Tên màu để hiển thị, ví dụ: "Xanh Navy", "Trắng"

    // [MỚI] Thêm trường để lưu URL ảnh đại diện cho màu
    @Column(name = "color_image_url", length = 500)
    private String colorImageUrl;

    @Column(name = "product_size", length = 20)
    private String productSize;

    @Column(unique = true, length = 100)
    private String sku;

    @Column(name = "stock_quantity")
    private Integer stockQuantity = 0;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "image_url", length = 500)
    private String imageUrl; // Ảnh chính khi chọn biến thể này
}