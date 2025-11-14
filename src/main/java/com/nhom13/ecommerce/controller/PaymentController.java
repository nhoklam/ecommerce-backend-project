package com.nhom13.ecommerce.controller;

import com.nhom13.ecommerce.dto.CreatePaymentRequestDTO;
import com.nhom13.ecommerce.dto.CreatePaymentResponseDTO;
import com.nhom13.ecommerce.entity.Order;
import com.nhom13.ecommerce.entity.PaymentStatus;
import com.nhom13.ecommerce.entity.PaymentTransaction;
import com.nhom13.ecommerce.exception.BadRequestException;
import com.nhom13.ecommerce.exception.ResourceNotFoundException;
import com.nhom13.ecommerce.repository.OrderRepository;
import com.nhom13.ecommerce.service.PaymentGatewayService;
import com.nhom13.ecommerce.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize; // <-- ĐÃ XÓA
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder; 
import java.nio.charset.StandardCharsets; 
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class PaymentController {
    
    @Qualifier("vnPayServiceImpl")
    private final PaymentGatewayService vnPayService;
    private final OrderRepository orderRepository;
    private final UserService userService;

    @PostMapping("/create")
    // @PreAuthorize("hasRole('CUSTOMER')") // <-- ĐÃ XÓA CHÚ THÍCH NÀY
    public ResponseEntity<CreatePaymentResponseDTO> createPayment(
            @Valid @RequestBody CreatePaymentRequestDTO requestDTO,
            Authentication authentication,
            HttpServletRequest request) {
        
        Long userId = userService.getUserByEmail(authentication.getName()).getId();
        Order order = orderRepository.findById(requestDTO.getOrderId())
           .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("You do not own this order.");
        }
        
        if (!"VNPAY".equalsIgnoreCase(order.getPaymentMethod())) {
            throw new BadRequestException("This order was not created for VNPAY payment.");
        }

        CreatePaymentResponseDTO response = vnPayService.createPaymentUrl(order, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/vnpay_return")
    public ResponseEntity<Void> vnpayReturn(
            @RequestParam Map<String, String> params,
            HttpServletRequest request) {
        
        log.info("VNPAY return URL called with params: {}", params);
        final String frontendUrl = "http://localhost:5173/payment-result";
        UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromHttpUrl(frontendUrl);
        
        try {
            PaymentTransaction transaction = vnPayService.handlePaymentCallback(params);
            // Xử lý thành công
            String status = transaction.getStatus() == PaymentStatus.SUCCESSFUL ? "success" : "failed";
            String message = transaction.getStatus() == PaymentStatus.SUCCESSFUL 
                             ? "Thanh toán đơn hàng #" + transaction.getOrder().getId() + " thành công!"
                             : "Thanh toán đơn hàng #" + transaction.getOrder().getId() + " thất bại.";
            
            urlBuilder.queryParam("status", status);
            urlBuilder.queryParam("message", message);
            urlBuilder.queryParam("orderId", transaction.getOrder().getId());
        } catch (Exception e) {
            log.error("Error processing VNPAY return: {}", e.getMessage());
            // Xử lý thất bại (lỗi hệ thống)
            urlBuilder.queryParam("status", "failed");
            urlBuilder.queryParam("message", "Có lỗi xảy ra trong quá trình xử lý thanh toán.");
        }
        
        // Tạo URL cuối cùng đã được encode đúng chuẩn
        String redirectUrl = urlBuilder.build().encode(StandardCharsets.UTF_8).toUriString();
        return ResponseEntity.status(302).header("Location", redirectUrl).build();
    }

    @GetMapping("/vnpay_ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(
            @RequestParam Map<String, String> params,
            HttpServletRequest request) {
        
        log.info("VNPAY IPN URL called with params: {}", params);
        try {
            vnPayService.handlePaymentCallback(params);
            return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
        } catch (Exception e) {
            log.error("Error processing VNPAY IPN: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("RspCode", "99", "Message", "Failed to process"));
        }
    }
}