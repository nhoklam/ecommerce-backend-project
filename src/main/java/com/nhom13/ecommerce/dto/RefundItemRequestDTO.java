package com.nhom13.ecommerce.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RefundItemRequestDTO {
    @NotNull(message = "Order Item ID is required")
    private Long orderItemId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1") // [12]
    private Integer quantity;
}