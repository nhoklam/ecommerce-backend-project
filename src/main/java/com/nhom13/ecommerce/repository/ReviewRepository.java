package com.nhom13.ecommerce.repository;

import com.nhom13.ecommerce.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

// import java.util.List; // [ĐÃ XÓA]

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    Page<Review> findByProductId(Long productId, Pageable pageable);
    
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    // Truy vấn để tính toán rating trung bình
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double calculateAverageRating(Long productId);
    
    // Truy vấn để kiểm tra xem user đã mua sản phẩm này (đơn hàng DELIVERED) chưa
    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi " +
           "WHERE oi.order.user.id = :userId " +
           "AND oi.product.id = :productId " +
           "AND oi.order.status = 'DELIVERED'")
    boolean didUserPurchaseProduct(Long userId, Long productId);

    // Thêm phương thức bị thiếu
    long countByProductId(Long productId);
}