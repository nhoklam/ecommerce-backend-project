package com.nhom13.ecommerce.service;

import com.nhom13.ecommerce.config.VnPayConfig;
import com.nhom13.ecommerce.dto.CreatePaymentResponseDTO;
import com.nhom13.ecommerce.dto.OrderDTO;
import com.nhom13.ecommerce.entity.*;
import com.nhom13.ecommerce.exception.BadRequestException;
import com.nhom13.ecommerce.exception.ResourceNotFoundException;
import com.nhom13.ecommerce.repository.CartItemRepository;
import com.nhom13.ecommerce.repository.OrderRepository;
import com.nhom13.ecommerce.repository.PaymentTransactionRepository;
import com.nhom13.ecommerce.util.VnPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VnPayServiceImpl implements PaymentGatewayService {

    private final VnPayConfig vnPayConfig;
    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final CartItemRepository cartItemRepository; // [SỬA ĐỔI] - Xóa (Không cần nữa)
    private final ProductService productService;
    private final EmailService emailService;
    private final OrderService orderService;

    @Override
    public String getGatewayName() {
        return "VNPAY";
    }

    @Override
    @Transactional
    public CreatePaymentResponseDTO createPaymentUrl(Order order, HttpServletRequest request) {
        if (!"PENDING".equals(order.getPaymentStatus()) || order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Order is not in a valid state for payment.");
        }

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrder(order);
        transaction.setAmount(order.getTotalAmount());
        transaction.setStatus(PaymentStatus.PENDING);
        transaction.setPaymentMethod(getGatewayName());
        transactionRepository.save(transaction);
        
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        long amount = order.getTotalAmount().multiply(new BigDecimal(100)).longValue();
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", order.getId().toString());
        vnp_Params.put("vnp_OrderInfo", buildOrderInfo(order.getOrderItems()));
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrlBase());
        vnp_Params.put("vnp_IpAddr", getIpAddress(request));
        vnp_Params.put("vnp_CreateDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        
        String hashData = VnPayUtil.buildQueryString(vnp_Params);
        String vnp_SecureHash = VnPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData);
        vnp_Params.put("vnp_SecureHash", vnp_SecureHash);

        String paymentUrl = vnPayConfig.getUrl() + "?" + VnPayUtil.buildQueryString(vnp_Params);
        return new CreatePaymentResponseDTO(paymentUrl, order.getId().toString());
    }

    @Override
    @Transactional
    public PaymentTransaction handlePaymentCallback(Map<String, String> params) {
        log.info("Handling VNPAY IPN callback: {}", params);
        if (!VnPayUtil.verifySignature(new HashMap<>(params), vnPayConfig.getHashSecret())) {
            log.error("VNPAY signature verification failed!");
            throw new BadRequestException("Invalid VNPAY signature");
        }

        String orderIdStr = params.get("vnp_TxnRef");
        String transactionNo = params.get("vnp_TransactionNo");
        String responseCode = params.get("vnp_ResponseCode");

        Order order = orderRepository.findById(Long.parseLong(orderIdStr))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderIdStr));
        
        if (!"PENDING".equals(order.getPaymentStatus())) {
            log.warn("Order {} already processed. Skipping IPN.", orderIdStr);
            PaymentStatus currentStatus = "PAID".equals(order.getPaymentStatus()) ? PaymentStatus.SUCCESSFUL : PaymentStatus.FAILED;
            return transactionRepository.findByOrderIdAndStatus(order.getId(), currentStatus)
                    .orElseThrow(() -> new ResourceNotFoundException("Processed transaction not found for order: " + orderIdStr));
        }

        PaymentTransaction transaction = transactionRepository.findByOrderIdAndStatus(order.getId(), PaymentStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("Pending transaction not found for order: " + orderIdStr));
        
        transaction.setTransactionId(transactionNo);
        transaction.setPayload(params);
        
        if ("00".equals(responseCode)) {
            log.info("Payment successful for order {}", orderIdStr);
            transaction.setStatus(PaymentStatus.SUCCESSFUL);
            order.setPaymentStatus("PAID");
            order.setStatus(OrderStatus.PROCESSING);

            // [SỬA ĐỔI] XÓA LOGIC TRỪ KHO VÀ XÓA GIỎ HÀNG
            try {
                // Chỉ gửi mail
                OrderDTO orderDTO = orderService.convertToDTO(order);
                emailService.sendOrderConfirmation(order.getUser().getEmail(), orderDTO);
            } catch (Exception e) {
                log.error("Failed to send confirmation email for order {}: {}", orderIdStr, e.getMessage());
            }

        } else {
            log.warn("Payment failed for order {} with code {}", orderIdStr, responseCode);
            transaction.setStatus(PaymentStatus.FAILED);
            order.setPaymentStatus("FAILED");
            order.setStatus(OrderStatus.CANCELLED);

            // [SỬA ĐỔI] THÊM LOGIC HOÀN KHO KHI THANH TOÁN THẤT BẠI
            try {
                log.info("Restoring stock for failed payment on order {}", orderIdStr);
                order.getOrderItems().forEach(item -> {
                    productService.restoreVariantStock(item.getProductVariant().getId(), item.getQuantity());
                });
            } catch (Exception e) {
                log.error("CRITICAL: Failed to restore stock for cancelled order {}: {}", orderIdStr, e.getMessage());
                // Ghi log lỗi nghiêm trọng, admin cần can thiệp thủ công
            }
        }

        orderRepository.save(order);
        return transactionRepository.save(transaction);
    }

    private String buildOrderInfo(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return "Payment for order";
        }
        String orderInfo = orderItems.stream()
                .map(item -> item.getProductVariant().getProduct().getName())
                .collect(Collectors.joining(", "));
        return orderInfo.length() > 255 ? orderInfo.substring(0, 252) + "..." : orderInfo;
    }

    private String getIpAddress(HttpServletRequest request) {
        String ipAddr = request.getHeader("X-Forwarded-For");
        if (ipAddr == null || ipAddr.isEmpty() || "unknown".equalsIgnoreCase(ipAddr)) {
            ipAddr = request.getRemoteAddr();
        }
        return ipAddr.split(",")[0].trim();
    }
}