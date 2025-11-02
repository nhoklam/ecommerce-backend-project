package com.nhom13.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "refund_items")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RefundItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_request_id", nullable = false)
    private RefundRequest refundRequest;

    /**
     * Liên kết Một-Một với OrderItem.
     * Mỗi dòng trong đơn hàng chỉ có thể được refund (hoặc đang được yêu cầu refund) một lần.
     * Chúng ta sẽ dùng trường refundedQuantity trong OrderItem để kiểm soát logic này,
     * nhưng liên kết này chỉ định rõ mặt hàng nào đang được yêu cầu.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(nullable = false)
    private Integer quantity; // Số lượng yêu cầu trả
}