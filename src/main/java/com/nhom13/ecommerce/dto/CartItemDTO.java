package com.nhom13.ecommerce.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CartItemDTO {
    
    private Long id;
    
    // ID của sản phẩm cha, hữu ích để có thể click vào xem lại sản phẩm gốc
    private Long productId; 
    
    // [MỚI] ID của biến thể cụ thể đã được chọn
    private Long productVariantId;
    
    private String productName;
    private BigDecimal unitPrice; // Giá của biến thể tại thời điểm thêm vào giỏ
    private Integer quantity;
    private BigDecimal totalPrice;
    private String imageUrl; // Ảnh của biến thể

    // [MỚI] Các thuộc tính của biến thể để hiển thị trên UI
    private String color;
    private String size;
}