package com.nhom13.ecommerce.dto;

import com.nhom13.ecommerce.entity.OrderItem;
import com.nhom13.ecommerce.entity.Product;
import com.nhom13.ecommerce.entity.RefundItem;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class RefundItemResponseDTO {
    private Long refundItemId;
    private Long orderItemId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;

    public static RefundItemResponseDTO fromEntity(RefundItem entity) {
        RefundItemResponseDTO dto = new RefundItemResponseDTO();
        dto.setRefundItemId(entity.getId());
        dto.setQuantity(entity.getQuantity());

        OrderItem orderItem = entity.getOrderItem();
        dto.setOrderItemId(orderItem.getId());
        
        // [SỬA] Lấy thông tin sản phẩm cha từ biến thể
        Product product = orderItem.getProductVariant().getProduct();
        dto.setProductName(product.getName());
        
        dto.setUnitPrice(orderItem.getUnitPrice());
        dto.setTotalPrice(orderItem.getUnitPrice().multiply(BigDecimal.valueOf(entity.getQuantity())));

        return dto;
    }
}