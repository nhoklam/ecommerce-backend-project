package com.nhom13.ecommerce.repository;

import com.nhom13.ecommerce.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Truy vấn bảo mật: Tìm một OrderItem bằng ID của nó và ID của User sở hữu đơn hàng.
     * Điều này ngăn người dùng A thao tác trên OrderItem của người dùng B.
     * Logic này vẫn đúng kể cả khi OrderItem liên kết với ProductVariant.
     */
    Optional<OrderItem> findByIdAndOrder_UserId(Long orderItemId, Long userId);
}