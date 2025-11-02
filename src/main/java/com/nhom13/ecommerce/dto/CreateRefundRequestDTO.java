package com.nhom13.ecommerce.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class CreateRefundRequestDTO {
    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotBlank(message = "Refund reason is required") // [13, 14]
    private String reason;

    @NotEmpty(message = "At least one item is required for refund")
    @Valid // Quan trọng: Kích hoạt xác thực lồng nhau cho các đối tượng bên trong List [15]
    private List<RefundItemRequestDTO> items;
}