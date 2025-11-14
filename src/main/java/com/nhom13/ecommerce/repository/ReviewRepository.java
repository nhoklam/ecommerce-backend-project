package com.nhom13.ecommerce.repository;

import com.nhom13.ecommerce.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByProductId(Long productId, Pageable pageable);

    @Query("""
        SELECT r FROM Review r
        JOIN FETCH r.user u
        JOIN FETCH r.product p
        WHERE r.product.id = :productId
        """)
    Page<Review> findByProductIdWithUser(@Param("productId") Long productId, Pageable pageable);

    @Query("""
        SELECT r FROM Review r
        JOIN FETCH r.user u
        JOIN FETCH r.product p
        WHERE r.product.id = :productId AND r.rating = :rating
        """)
    Page<Review> findByProductIdAndRatingWithUser(@Param("productId") Long productId,
            @Param("rating") int rating,
            Pageable pageable);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double calculateAverageRating(@Param("productId") Long productId);

    /**
     * Kiểm tra xem một user đã mua và nhận một sản phẩm cụ thể hay chưa.
     */
    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi " +
           "WHERE oi.order.user.id = :userId " +
           // [SỬA] Thay đổi đường dẫn truy vấn để đi qua biến thể sản phẩm
           "AND oi.productVariant.product.id = :productId " +
           "AND oi.order.status = 'DELIVERED'")
    boolean didUserPurchaseProduct(@Param("userId") Long userId, @Param("productId") Long productId);

    long countByProductId(Long productId);

    long countByProductIdAndRating(Long productId, int rating);
}