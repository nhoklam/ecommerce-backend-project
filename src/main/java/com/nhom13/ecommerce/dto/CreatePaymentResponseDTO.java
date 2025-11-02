package com.nhom13.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentResponseDTO {
    private String paymentUrl; // URL để chuyển hướng người dùng
    private String orderId;    // ID đơn hàng
}