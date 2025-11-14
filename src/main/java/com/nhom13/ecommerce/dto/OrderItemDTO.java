package com.nhom13.ecommerce.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemDTO {

    private Long id;

    // ID của sản phẩm cha
    private Long productId;

    // [MỚI] ID của biến thể cụ thể đã được đặt hàng
    private Long productVariantId;

    // Tên sản phẩm, có thể được service ghép thêm thông tin màu/size
    private String productName;

    private Integer quantity;
    private BigDecimal unitPrice; // Giá của biến thể tại thời điểm đặt hàng
    private BigDecimal totalPrice;
    
    private boolean isReviewed;
    
    // [XÓA] Trường stockQuantity không cần thiết ở đây.
    // private Integer stockQuantity;
}