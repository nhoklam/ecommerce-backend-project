package com.nhom13.ecommerce.service;

import com.nhom13.ecommerce.config.VnPayConfig;
import com.nhom13.ecommerce.dto.CreatePaymentResponseDTO;
import com.nhom13.ecommerce.dto.OrderDTO;
// Cần DTO này
import com.nhom13.ecommerce.entity.*; // Cần Order, PaymentTransaction, PaymentStatus
import com.nhom13.ecommerce.exception.BadRequestException;
import com.nhom13.ecommerce.exception.ResourceNotFoundException;
import com.nhom13.ecommerce.repository.CartItemRepository; // Cần repo này
import com.nhom13.ecommerce.repository.OrderRepository;
import com.nhom13.ecommerce.repository.PaymentTransactionRepository;
import com.nhom13.ecommerce.util.VnPayUtil; // Cần util bảo mật
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VnPayServiceImpl implements PaymentGatewayService {

    private final VnPayConfig vnPayConfig;
private final OrderRepository orderRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final CartItemRepository cartItemRepository;
// Sửa đổi: Cần thiết cho tác dụng phụ
    private final ProductService productService;
// [1]
    private final EmailService emailService;     // [1]
    private final OrderService orderService;
// Sửa đổi: Cần thiết để gọi convertToDTO

    @Override
    public String getGatewayName() {
        return "VNPAY";
}

    @Override
    @Transactional
    public CreatePaymentResponseDTO createPaymentUrl(Order order, HttpServletRequest request) {
        // Logic nghiệp vụ: Chỉ tạo thanh toán cho đơn hàng PENDING
        //
        if (!"PENDING".equals(order.getPaymentStatus()) || order.getStatus()!= OrderStatus.PENDING) {
            throw new BadRequestException("Order is not in a valid state for payment.");
}
        
        // Tạo một giao dịch PENDING mới
        PaymentTransaction transaction = new PaymentTransaction();
transaction.setOrder(order);
        transaction.setAmount(order.getTotalAmount());
        transaction.setStatus(PaymentStatus.PENDING);
        transaction.setPaymentMethod(getGatewayName());
        transactionRepository.save(transaction); // Lưu giao dịch PENDING

        // Xây dựng các tham số VNPAY [2]
        Map<String, String> vnp_Params = new HashMap<>();
vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        
        // VNPAY yêu cầu số tiền là số nguyên (nhân 100, đơn vị đồng)
        long amount = order.getTotalAmount().multiply(new BigDecimal(100)).longValue();
vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        
        // vnp_TxnRef phải là duy nhất. Chúng ta dùng Order ID.
        vnp_Params.put("vnp_TxnRef", order.getId().toString());
vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang #" + order.getId());
        vnp_Params.put("vnp_OrderType", "other");
// [2]
        vnp_Params.put("vnp_Locale", "vn");
// [2]
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrlBase());
        vnp_Params.put("vnp_IpAddr", getIpAddress(request));

        LocalDateTime createDate = LocalDateTime.now();
        vnp_Params.put("vnp_CreateDate", createDate.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
// Tạo chữ ký
        String hashData = VnPayUtil.buildQueryString(vnp_Params);
        String vnp_SecureHash = VnPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData);
vnp_Params.put("vnp_SecureHash", vnp_SecureHash);

        // Xây dựng URL đầy đủ (đã bao gồm chữ ký)
        String paymentUrl = vnPayConfig.getUrl() + "?"
+ VnPayUtil.buildQueryString(vnp_Params);
        
        return new CreatePaymentResponseDTO(paymentUrl, order.getId().toString());
    }

    @Override
    @Transactional
    public PaymentTransaction handlePaymentCallback(Map<String, String> params) {
        log.info("Handling VNPAY IPN callback: {}", params);
// 1. Xác minh chữ ký [3, 4]
        // Tạo bản sao của map để xác minh, vì hàm verifySignature sẽ sửa đổi map
        if (!VnPayUtil.verifySignature(new HashMap<>(params), vnPayConfig.getHashSecret())) {
            log.error("VNPAY signature verification failed!");
throw new BadRequestException("Invalid VNPAY signature");
        }

        // 2. Lấy thông tin
        String orderIdStr = params.get("vnp_TxnRef");
String transactionNo = params.get("vnp_TransactionNo"); // ID của VNPAY
        String responseCode = params.get("vnp_ResponseCode");
// String amount = params.get("vnp_Amount"); // Có thể kiểm tra amount nếu cần

        Order order = orderRepository.findById(Long.parseLong(orderIdStr))
          .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderIdStr));
// 3. Kiểm tra Tính toàn vẹn (Idempotency) [5, 6]
        if (!"PENDING".equals(order.getPaymentStatus())) {
            log.warn("Order {} already processed. Skipping IPN.", orderIdStr);
PaymentStatus currentStatus = "PAID".equals(order.getPaymentStatus())? 
                                          PaymentStatus.SUCCESSFUL : PaymentStatus.FAILED;
            return transactionRepository.findByOrderIdAndStatus(order.getId(), currentStatus)
              .orElseThrow(() -> new ResourceNotFoundException("Processed transaction not found for order: " + orderIdStr));
}
        
        // 4. Lấy giao dịch PENDING chúng ta đã tạo
        //
        PaymentTransaction transaction = transactionRepository.findByOrderIdAndStatus(order.getId(), PaymentStatus.PENDING)
          .orElseThrow(() -> new ResourceNotFoundException("Pending transaction not found for order: " + orderIdStr));
// 5. Cập nhật giao dịch
        transaction.setTransactionId(transactionNo);
        transaction.setPayload(params);
// Lưu trữ toàn bộ dữ liệu VNPAY [7]
        
        // 6. Xử lý Logic Nghiệp vụ
        if ("00".equals(responseCode)) {
            // Thanh toán THÀNH CÔNG [8, 9, 10]
            log.info("Payment successful for order {}", orderIdStr);
transaction.setStatus(PaymentStatus.SUCCESSFUL);
            
            order.setPaymentStatus("PAID");
            order.setStatus(OrderStatus.PROCESSING); // Chuyển sang "Đang xử lý"
            
            // 7. Kích hoạt các tác dụng phụ (Side Effects)
            try {
                // 7a.
                order.getOrderItems().forEach(item -> {
                    productService.updateStock(item.getProduct().getId(), item.getQuantity());
                });
// 7b. Xóa giỏ hàng [1]
                cartItemRepository.deleteAllByUserId(order.getUser().getId());
// 7c. Gửi Email [1][11]
                OrderDTO orderDTO = orderService.convertToDTO(order);
emailService.sendOrderConfirmation(order.getUser().getEmail(), orderDTO);
            
            } catch (Exception e) {
                log.error("Failed to execute post-payment side effects for order {}: {}", orderIdStr, e.getMessage());
}

        } else {
            // Thanh toán THẤT BẠI
            log.warn("Payment failed for order {} with code {}", orderIdStr, responseCode);
transaction.setStatus(PaymentStatus.FAILED);
            order.setPaymentStatus("FAILED");
            order.setStatus(OrderStatus.CANCELLED); 
        }

        orderRepository.save(order);
        return transactionRepository.save(transaction);
}

    private String getIpAddress(HttpServletRequest request) {
        String ipAddr = request.getHeader("X-Forwarded-For");
//
        if (ipAddr == null || ipAddr.isEmpty() || "unknown".equalsIgnoreCase(ipAddr)) {
            ipAddr = request.getRemoteAddr();
}
        // Sửa lỗi: Lấy phần tử đầu tiên của mảng và trim() nó
        return ipAddr.split(",")[0].trim();
}
}