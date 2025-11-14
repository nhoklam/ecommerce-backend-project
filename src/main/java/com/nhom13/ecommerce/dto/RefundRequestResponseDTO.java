package com.nhom13.ecommerce.dto;

import com.nhom13.ecommerce.entity.RefundRequest;
import com.nhom13.ecommerce.entity.RefundStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class RefundRequestResponseDTO {
    private Long id;
    private Long orderId;
    private Long userId;
    private String userEmail;
    private String userName; // <-- [1] ĐÃ THÊM TRƯỜNG MỚI
    private RefundStatus status;
    private String reason;
    private String adminNotes;
    private BigDecimal totalRefundAmount;
    private LocalDateTime createdAt;
    private List<RefundItemResponseDTO> items;

    // Helper mapper (ánh xạ lồng nhau)
    public static RefundRequestResponseDTO fromEntity(RefundRequest entity) {
        RefundRequestResponseDTO dto = new RefundRequestResponseDTO();
        dto.setId(entity.getId());
        dto.setOrderId(entity.getOrder().getId());
        dto.setUserId(entity.getUser().getId());
        dto.setUserEmail(entity.getUser().getEmail());
        
        // [2] ĐÃ THÊM LOGIC LẤY TÊN ĐẦY ĐỦ TỪ USER ENTITY
        dto.setUserName(entity.getUser().getFullName()); 
        
        dto.setStatus(entity.getStatus());
        dto.setReason(entity.getReason());
        dto.setAdminNotes(entity.getAdminNotes());
        dto.setTotalRefundAmount(entity.getTotalRefundAmount());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setItems(
            entity.getItems().stream()
               .map(RefundItemResponseDTO::fromEntity)
               .collect(Collectors.toList())
        );
        return dto;
    }
}