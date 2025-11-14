package com.nhom13.ecommerce.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductVariantDTO {
    private Long id;
    private Long productId;
    private String color;
    private String colorImageUrl; 
    private String productSize;
    private String sku;
    private Integer stockQuantity;
    private BigDecimal price;
    private String imageUrl;
}