package com.nhom13.ecommerce.repository;

import com.nhom13.ecommerce.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    
    List<CartItem> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);
    
    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.user.id = :userId")
    void deleteAllByUserId(Long userId);
    
    @Query("SELECT SUM(c.quantity * c.product.price) FROM CartItem c WHERE c.user.id = :userId")
    java.math.BigDecimal calculateCartTotal(Long userId);
}