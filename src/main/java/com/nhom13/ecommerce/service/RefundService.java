package com.nhom13.ecommerce.service;

import com.nhom13.ecommerce.dto.CreateRefundRequestDTO;
import com.nhom13.ecommerce.dto.RefundItemRequestDTO;
import com.nhom13.ecommerce.dto.RefundRequestResponseDTO;
import com.nhom13.ecommerce.entity.*;
import com.nhom13.ecommerce.exception.BadRequestException;
import com.nhom13.ecommerce.exception.ResourceNotFoundException;
import com.nhom13.ecommerce.repository.OrderItemRepository;
import com.nhom13.ecommerce.repository.OrderRepository;
import com.nhom13.ecommerce.repository.RefundRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    private final RefundRequestRepository refundRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository; // Repository mới
    private final ProductService productService; // Service hiện có
    private final EmailService emailService; // Service hiện có

    /**
     * Tạo một yêu cầu trả hàng mới.
     * Đây là một giao dịch (transaction) phức tạp, phải đảm bảo tính toàn vẹn dữ liệu.[18, 19]
     */
    @Transactional
    public RefundRequestResponseDTO createRefundRequest(Long userId, CreateRefundRequestDTO dto) {
        log.info("User {} creating refund request for order {}", userId, dto.getOrderId());

        // 1. Lấy và xác thực Đơn hàng
        Order order = orderRepository.findById(dto.getOrderId())
               .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + dto.getOrderId()));

        // 2. Xác thực quyền sở hữu
        if (!order.getUser().getId().equals(userId)) {
            log.warn("Access Denied: User {} attempt to refund order {} owned by {}", userId, order.getId(), order.getUser().getId());
            throw new AccessDeniedException("You do not own this order.");
        }

        // 3. Xác thực trạng thái đơn hàng (Business Rule) [20]
        if (order.getStatus()!= OrderStatus.DELIVERED) {
            log.warn("Bad Request: User {} attempt to refund order {} with status {}", userId, order.getId(), order.getStatus());
            throw new BadRequestException("Refunds are only allowed for DELIVERED orders. Current status: " + order.getStatus());
        }

        // 4. Chuẩn bị tạo yêu cầu
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setUser(order.getUser());
        refundRequest.setOrder(order);
        refundRequest.setReason(dto.getReason());
        refundRequest.setStatus(RefundStatus.PENDING);

        List<RefundItem> refundItemsList = new ArrayList<>();
        BigDecimal totalRefundAmount = BigDecimal.ZERO;

        // 5. Lặp qua các mặt hàng yêu cầu và xác thực [21, 22]
        for (RefundItemRequestDTO itemDTO : dto.getItems()) {
            OrderItem orderItem = orderItemRepository.findByIdAndOrder_UserId(itemDTO.getOrderItemId(), userId)
                   .orElseThrow(() -> new ResourceNotFoundException("Order Item not found or does not belong to you: " + itemDTO.getOrderItemId()));

            int requestedQty = itemDTO.getQuantity();
            int availableQty = orderItem.getQuantity() - orderItem.getRefundedQuantity();

            // 6. Xác thực số lượng (Business Rule)
            if (requestedQty > availableQty) {
                log.warn("Bad Request: User {} requested qty {} for item {} but only {} available", userId, requestedQty, orderItem.getId(), availableQty);
                throw new BadRequestException(String.format(
                    "Invalid quantity for item '%s'. Max available for refund: %d",
                    orderItem.getProduct().getName(), availableQty
                ));
            }

            // 7. Cập nhật số lượng "đã/đang trả"
            orderItem.setRefundedQuantity(orderItem.getRefundedQuantity() + requestedQty);
            // (Không cần save, @Transactional sẽ tự động cập nhật [23])

            // 8. Tạo RefundItem
            RefundItem refundItem = new RefundItem();
            refundItem.setRefundRequest(refundRequest);
            refundItem.setOrderItem(orderItem);
            refundItem.setQuantity(requestedQty);
            refundItemsList.add(refundItem);

            // 9. Tính tổng số tiền refund
            totalRefundAmount = totalRefundAmount.add(
                orderItem.getUnitPrice().multiply(BigDecimal.valueOf(requestedQty))
            );
        }

        // 10. Lưu RefundRequest (và các RefundItem con nhờ CascadeType.ALL)
        refundRequest.setItems(refundItemsList);
        refundRequest.setTotalRefundAmount(totalRefundAmount);
        RefundRequest savedRefundRequest = refundRepository.save(refundRequest);

        log.info("Successfully created RefundRequest ID: {} for Order ID: {}", savedRefundRequest.getId(), order.getId());

        // 11. Gửi email thông báo
        try {
            emailService.sendRefundConfirmation(order.getUser().getEmail(), savedRefundRequest);
        } catch (Exception e) {
            log.error("Failed to send refund confirmation email for refundId: {}", savedRefundRequest.getId(), e);
        }

        return RefundRequestResponseDTO.fromEntity(savedRefundRequest);
    }

    /**
     * Cập nhật trạng thái của một yêu cầu trả hàng.
     * Logic này cũng phải là @Transactional.
     */
    @Transactional
    public RefundRequestResponseDTO updateRefundStatus(Long refundId, RefundStatus newStatus, String adminNotes) {
        log.info("Admin updating refund {} to status {}", refundId, newStatus);
        RefundRequest refund = refundRepository.findById(refundId)
               .orElseThrow(() -> new ResourceNotFoundException("RefundRequest not found: " + refundId));

        if (refund.getStatus() == newStatus) {
            return RefundRequestResponseDTO.fromEntity(refund); // Không thay đổi
        }

        // Logic hoàn trả hàng hóa / số lượng
        switch (newStatus) {
            case COMPLETED:
                // Hoàn tất: Cộng hàng lại vào kho
                for (RefundItem item : refund.getItems()) {
                    productService.restoreStock(item.getOrderItem().getProduct().getId(), item.getQuantity());
                    log.info("Restored stock for Product ID {}: +{}", item.getOrderItem().getProduct().getId(), item.getQuantity());
                }
                break;

            case REJECTED:
                // Từ chối: Hoàn lại "số lượng có thể trả" cho OrderItem
                for (RefundItem item : refund.getItems()) {
                    OrderItem orderItem = item.getOrderItem();
                    orderItem.setRefundedQuantity(orderItem.getRefundedQuantity() - item.getQuantity());
                    // @Transactional sẽ lo việc save [23]
                    log.info("Restored 'refundable_quantity' for OrderItem ID {}: +{}", orderItem.getId(), item.getQuantity());
                }
                break;

            case PENDING:
            case PROCESSING:
                // Không cần hành động đặc biệt về kho bãi
                break;
        }

        refund.setStatus(newStatus);
        refund.setAdminNotes(adminNotes);
        RefundRequest updatedRefund = refundRepository.save(refund);

        // Gửi email thông báo cập nhật
        try {
            emailService.sendRefundStatusUpdate(refund.getUser().getEmail(), updatedRefund);
        } catch (Exception e) {
            log.error("Failed to send refund status update email for refundId: {}", updatedRefund.getId(), e);
        }

        return RefundRequestResponseDTO.fromEntity(updatedRefund);
    }

    /**
     * Lấy các yêu cầu refund của một user.
     */
    @Transactional(readOnly = true)
    public List<RefundRequestResponseDTO> getRefundsForUser(Long userId) {
        return refundRepository.findByUserId(userId).stream()
               .map(RefundRequestResponseDTO::fromEntity)
               .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả yêu cầu refund (phân trang).
     */
    @Transactional(readOnly = true)
    public Page<RefundRequestResponseDTO> getAllRefunds(Pageable pageable) {
        return refundRepository.findAll(pageable)
               .map(RefundRequestResponseDTO::fromEntity);
    }

    /**
     * Lấy chi tiết một yêu cầu.
     */
    @Transactional(readOnly = true)
    public RefundRequestResponseDTO getRefundById(Long refundId, Long userId) {
        RefundRequest refund = refundRepository.findById(refundId)
               .orElseThrow(() -> new ResourceNotFoundException("RefundRequest not found: " + refundId));

        // Xác thực quyền sở hữu (Admin bỏ qua bước này, xử lý ở Controller)
        if (!refund.getUser().getId().equals(userId)) {
            // Giả sử có một vai trò Admin được truyền vào (xử lý ở Controller)
            // Trong service, chúng ta chỉ kiểm tra
            log.warn("Access Denied: User {} attempt to access refund {} owned by {}", userId, refund.getId(), refund.getUser().getId());
            throw new AccessDeniedException("You do not own this refund request.");
        }

        return RefundRequestResponseDTO.fromEntity(refund);
    }
}