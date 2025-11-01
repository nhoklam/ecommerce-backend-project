package com.nhom13.ecommerce.dto;

import lombok.Data;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map; // [MỚI] Import

@Data
public class ProductDTO {
    private Long id;

    @NotBlank(message = "Product name is required")
    private String name;
    
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;
    
    private String sku;

    @NotNull(message = "Category ID is required")
    private Long categoryId;
    
    private String categoryName;
    private String imageUrl;
    private Boolean isActive;

    // [MỚI]
    private Double averageRating;
    private Integer reviewCount;
    private Boolean isFeatured;
    private Map<String, String> specifications;
}