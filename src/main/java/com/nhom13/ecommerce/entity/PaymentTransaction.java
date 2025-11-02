package com.nhom13.ecommerce.entity;

import com.nhom13.ecommerce.util.JpaMapConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.Map;

@Entity
@Table(name = "payment_transactions")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaymentTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Mã giao dịch từ cổng thanh toán (ví dụ: vnp_TransactionNo)
    @Column(name = "transaction_id", unique = true)
    private String transactionId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING) // [16, 17, 18]
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private String paymentMethod; // "VNPAY", "COD", "STRIPE"

    // Lưu toàn bộ callback/IPN payload (dưới dạng JSON)
    @Column(columnDefinition = "TEXT")
    @Convert(converter = JpaMapConverter.class) // [13]
    private Map<String, String> payload;
}