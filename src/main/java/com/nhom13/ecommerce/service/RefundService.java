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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    private final RefundRequestRepository refundRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductService productService;
    private final EmailService emailService;

    @Transactional
    public RefundRequestResponseDTO createRefundRequest(Long userId, CreateRefundRequestDTO dto) {
        log.info("User {} creating refund request for order {}", userId, dto.getOrderId());

        Order order = orderRepository.findById(dto.getOrderId())
               .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + dto.getOrderId()));

        if (!order.getUser().getId().equals(userId)) {
            log.warn("Access Denied: User {} attempt to refund order {} owned by {}", userId, order.getId(), order.getUser().getId());
            throw new AccessDeniedException("You do not own this order.");
        }

        if (order.getStatus()!= OrderStatus.DELIVERED) {
            log.warn("Bad Request: User {} attempt to refund order {} with status {}", userId, order.getId(), order.getStatus());
            throw new BadRequestException("Refunds are only allowed for DELIVERED orders. Current status: " + order.getStatus());
        }

        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setUser(order.getUser());
        refundRequest.setOrder(order);
        refundRequest.setReason(dto.getReason());
        refundRequest.setStatus(RefundStatus.PENDING);

        List<RefundItem> refundItemsList = new ArrayList<>();
        BigDecimal totalRefundAmount = BigDecimal.ZERO;

        for (RefundItemRequestDTO itemDTO : dto.getItems()) {
            OrderItem orderItem = orderItemRepository.findByIdAndOrder_UserId(itemDTO.getOrderItemId(), userId)
                   .orElseThrow(() -> new ResourceNotFoundException("Order Item not found or does not belong to you: " + itemDTO.getOrderItemId()));

            int requestedQty = itemDTO.getQuantity();
            int availableQty = orderItem.getQuantity() - orderItem.getRefundedQuantity();

            if (requestedQty > availableQty) {
                log.warn("Bad Request: User {} requested qty {} for item {} but only {} available", userId, requestedQty, orderItem.getId(), availableQty);
                // [SỬA] Lấy tên sản phẩm thông qua variant
                String productName = orderItem.getProductVariant().getProduct().getName();
                throw new BadRequestException(String.format(
                    "Invalid quantity for item '%s'. Max available for refund: %d",
                    productName, availableQty
                ));
            }

            orderItem.setRefundedQuantity(orderItem.getRefundedQuantity() + requestedQty);

            RefundItem refundItem = new RefundItem();
            refundItem.setRefundRequest(refundRequest);
            refundItem.setOrderItem(orderItem);
            refundItem.setQuantity(requestedQty);
            refundItemsList.add(refundItem);

            totalRefundAmount = totalRefundAmount.add(
                orderItem.getUnitPrice().multiply(BigDecimal.valueOf(requestedQty))
            );
        }

        refundRequest.setItems(refundItemsList);
        refundRequest.setTotalRefundAmount(totalRefundAmount);
        RefundRequest savedRefundRequest = refundRepository.save(refundRequest);

        log.info("Successfully created RefundRequest ID: {} for Order ID: {}", savedRefundRequest.getId(), order.getId());

        try {
            emailService.sendRefundConfirmation(order.getUser().getEmail(), savedRefundRequest);
        } catch (Exception e) {
            log.error("Failed to send refund confirmation email for refundId: {}", savedRefundRequest.getId(), e);
        }

        return RefundRequestResponseDTO.fromEntity(savedRefundRequest);
    }

    @Transactional
    public RefundRequestResponseDTO updateRefundStatus(Long refundId, RefundStatus newStatus, String adminNotes) {
        log.info("Admin updating refund {} to status {}", refundId, newStatus);
        RefundRequest refund = refundRepository.findById(refundId)
               .orElseThrow(() -> new ResourceNotFoundException("RefundRequest not found: " + refundId));

        if (refund.getStatus() == newStatus) {
            return RefundRequestResponseDTO.fromEntity(refund);
        }

        switch (newStatus) {
            case COMPLETED:
                for (RefundItem item : refund.getItems()) {
                    // [SỬA] Gọi phương thức restoreVariantStock mới
                    Long variantId = item.getOrderItem().getProductVariant().getId();
                    productService.restoreVariantStock(variantId, item.getQuantity());
                    log.info("Restored stock for ProductVariant ID {}: +{}", variantId, item.getQuantity());
                }
                break;

            case REJECTED:
                for (RefundItem item : refund.getItems()) {
                    OrderItem orderItem = item.getOrderItem();
                    orderItem.setRefundedQuantity(orderItem.getRefundedQuantity() - item.getQuantity());
                    log.info("Restored 'refundable_quantity' for OrderItem ID {}: +{}", orderItem.getId(), item.getQuantity());
                }
                break;
            default:
                break;
        }

        refund.setStatus(newStatus);
        refund.setAdminNotes(adminNotes);
        RefundRequest updatedRefund = refundRepository.save(refund);

        try {
            emailService.sendRefundStatusUpdate(refund.getUser().getEmail(), updatedRefund);
        } catch (Exception e) {
            log.error("Failed to send refund status update email for refundId: {}", updatedRefund.getId(), e);
        }

        return RefundRequestResponseDTO.fromEntity(updatedRefund);
    }

    @Transactional(readOnly = true)
    public List<RefundRequestResponseDTO> getRefundsForUser(Long userId) {
        return refundRepository.findByUserId(userId).stream()
               .map(RefundRequestResponseDTO::fromEntity)
               .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<RefundRequestResponseDTO> getAllRefunds(Pageable pageable) {
        return refundRepository.findAll(pageable)
               .map(RefundRequestResponseDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public RefundRequestResponseDTO getRefundById(Long refundId, Long userId) {
        RefundRequest refund = refundRepository.findById(refundId)
               .orElseThrow(() -> new ResourceNotFoundException("RefundRequest not found: " + refundId));

        if (!refund.getUser().getId().equals(userId)) {
            log.warn("Access Denied: User {} attempt to access refund {} owned by {}", userId, refund.getId(), refund.getUser().getId());
            throw new AccessDeniedException("You do not own this refund request.");
        }

        return RefundRequestResponseDTO.fromEntity(refund);
    }
}