package com.nhom13.ecommerce.service;

import com.nhom13.ecommerce.dto.CreatePaymentResponseDTO;
import com.nhom13.ecommerce.entity.Order;
import com.nhom13.ecommerce.entity.PaymentTransaction;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public interface PaymentGatewayService {
    
    /**
     * Tạo URL thanh toán để chuyển hướng người dùng.
     */
    CreatePaymentResponseDTO createPaymentUrl(Order order, HttpServletRequest request);
    
    /**
     * Xử lý callback (IPN hoặc Return) từ cổng thanh toán.
     * Phương thức này phải CÓ TÍNH IDEMPOTENT (an toàn khi gọi lại).
     */
    PaymentTransaction handlePaymentCallback(Map<String, String> params);
    
    /**
     * Trả về tên định danh của cổng thanh toán (ví dụ: "VNPAY").
     */
    String getGatewayName(); 
}