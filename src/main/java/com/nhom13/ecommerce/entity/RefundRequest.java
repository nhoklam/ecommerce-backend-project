package com.nhom13.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "refund_requests")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true) // Cần thiết khi extends BaseEntity [4, 5]
public class RefundRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING) // [6, 7]
    @Column(nullable = false, length = 50)
    private RefundStatus status;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason; // Lý do trả hàng

    @Column(name = "total_refund_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalRefundAmount;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes; // Ghi chú của Admin khi duyệt/từ chối

    /**
     * Liên kết một-nhiều (bidirectional) đến các mặt hàng cụ thể trong yêu cầu này.
     * CascadeType.ALL đảm bảo các RefundItem con được lưu/xóa cùng với RefundRequest cha.[8]
     */
    @OneToMany(mappedBy = "refundRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<RefundItem> items;
}