package com.nhom13.ecommerce.dto;

import lombok.Data;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List; // [MỚI] Import List
import java.util.Map;

@Data
public class ProductDTO {
    
    private Long id;

    @NotBlank(message = "Product name is required")
    private String name;
    
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price; // Đây là giá gốc/giá niêm yết của sản phẩm

    // Trường này giờ đây là tổng hợp tồn kho từ các biến thể, không cần validation ở đây
    private Integer stockQuantity;
    
    // SKU ở cấp độ sản phẩm cha có thể không bắt buộc, vì SKU chi tiết nằm ở biến thể
    private String sku;

    @NotNull(message = "Category ID is required")
    private Long categoryId;
    
    private String categoryName;
    private String imageUrl; // Ảnh đại diện chung
    private Boolean isActive;

    // Các trường đã có
    private Double averageRating;
    private Integer reviewCount;
    private Boolean isFeatured;
    private Map<String, String> specifications;

    // ================== CÁC TRƯỜNG MỚI ==================

    // [MỚI] Thêm trường lượt xem
    private Long viewCount;
    
    // [MỚI] Thêm danh sách các biến thể của sản phẩm
    private List<ProductVariantDTO> variants;
}