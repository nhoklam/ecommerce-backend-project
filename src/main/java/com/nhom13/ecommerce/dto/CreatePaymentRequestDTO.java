package com.nhom13.ecommerce.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreatePaymentRequestDTO {
    @NotNull(message = "Order ID is required")
    private Long orderId;
}