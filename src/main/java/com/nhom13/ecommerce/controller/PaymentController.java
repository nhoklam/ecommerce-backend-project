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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class PaymentController {
    
    // Inject interface. Nếu có nhiều cổng (Momo, Stripe),
    // chúng ta sẽ inject List<PaymentGatewayService> và tìm theo tên.
    // Hiện tại, chúng ta inject trực tiếp bean VnPayServiceImpl.
    @Qualifier("vnPayServiceImpl") // Chỉ định rõ ràng bean
    private final PaymentGatewayService vnPayService;
    
    private final OrderRepository orderRepository;
    private final UserService userService; // 

    /**
     * Endpoint cho Client (đã xác thực) gọi để tạo URL thanh toán VNPAY.
     * [37, 38, 39, 40]
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CreatePaymentResponseDTO> createPayment(
            @Valid @RequestBody CreatePaymentRequestDTO requestDTO,
            Authentication authentication,
            HttpServletRequest request) {
        
        Long userId = userService.getUserByEmail(authentication.getName()).getId();
        Order order = orderRepository.findById(requestDTO.getOrderId())
           .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Xác minh quyền sở hữu đơn hàng
        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("You do not own this order.");
        }
        
        // Xác minh phương thức thanh toán của đơn hàng
        if (!"VNPAY".equalsIgnoreCase(order.getPaymentMethod())) {
            throw new BadRequestException("This order was not created for VNPAY payment.");
        }

        // Gọi service để tạo URL
        CreatePaymentResponseDTO response = vnPayService.createPaymentUrl(order, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint (public) mà trình duyệt của người dùng được VNPAY chuyển hướng về.
     * [25, 41, 42, 43]
     */
    @GetMapping("/vnpay_return")
    public ResponseEntity<Void> vnpayReturn(
            @RequestParam Map<String, String> params, // Lấy tất cả params
            HttpServletRequest request) {
        
        log.info("VNPAY return URL called with params: {}", params);
        
        PaymentTransaction transaction;
        try {
            // Xử lý callback. Logic Idempotency trong service sẽ ngăn xử lý trùng lặp
            // (nếu IPN đã chạy trước).
            transaction = vnPayService.handlePaymentCallback(params);
        } catch (Exception e) {
            log.error("Error processing VNPAY return: {}", e.getMessage());
            // Chuyển hướng đến trang thất bại của frontend
            // (Thay đổi URL này thành URL frontend của bạn)
            return ResponseEntity.status(302)
               .header("Location", "http://localhost:3000/payment-result?status=failed")
               .build();
        }

        // Chuyển hướng người dùng đến trang kết quả của frontend
        // (Thay đổi URL này thành URL frontend của bạn)
        String frontendUrl = "http://localhost:3000/payment-result"; 
        String redirectUrl = String.format("%s?orderId=%s&status=%s", 
            frontendUrl, 
            transaction.getOrder().getId(),
            transaction.getStatus() == PaymentStatus.SUCCESSFUL? "success" : "failed");
        
        // Trả về mã 302 (Redirect)
        return ResponseEntity.status(302).header("Location", redirectUrl).build();
    }

    /**
     * Endpoint (public) mà MÁY CHỦ VNPAY gọi (server-to-server).
     * Đây là "Nguồn chân lý" (Source of Truth).
     * [25, 44, 45]
     */
    @GetMapping("/vnpay_ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(
            @RequestParam Map<String, String> params, // Lấy tất cả params
            HttpServletRequest request) {
        
        log.info("VNPAY IPN URL called with params: {}", params);
        
        try {
            // Xử lý callback (xác minh, cập nhật DB, trừ kho, gửi email)
            vnPayService.handlePaymentCallback(params);
            
            // Phản hồi bắt buộc cho VNPAY nếu thành công 
            // "00" có nghĩa là "đã nhận và xử lý thành công"
            return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
        
        } catch (Exception e) {
            log.error("Error processing VNPAY IPN: {}", e.getMessage());
            // Phản hồi cho VNPAY nếu thất bại (ví dụ: chữ ký sai, không tìm thấy đơn hàng)
            // VNPAY sẽ thử gửi lại IPN nếu nhận được mã khác "00"
            return ResponseEntity.ok(Map.of("RspCode", "99", "Message", "Failed to process"));
        }
    }
}