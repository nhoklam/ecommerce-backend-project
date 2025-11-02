package com.nhom13.ecommerce.repository;

import com.nhom13.ecommerce.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Truy vấn bảo mật: Tìm một OrderItem bằng ID CỦA NÓ và ID CỦA USER sở hữu đơn hàng.
     * Điều này ngăn người dùng A yêu cầu trả hàng cho OrderItem của người dùng B.
     */
    Optional<OrderItem> findByIdAndOrder_UserId(Long orderItemId, Long userId);
}