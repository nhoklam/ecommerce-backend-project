package com.nhom13.ecommerce.repository;

import com.nhom13.ecommerce.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    
    //
    List<CartItem> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);
    
    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.user.id = :userId")
    // Đã sửa lỗi cú pháp
    void deleteAllByUserId(Long userId);
    
    @Query("SELECT SUM(c.quantity * c.product.price) FROM CartItem c WHERE c.user.id = :userId")
    BigDecimal calculateCartTotal(Long userId);

    // [MỚI] Các phương thức cho Guest Cart
    List<CartItem> findByTempCartIdOrderByCreatedAtDesc(String tempCartId);
    Optional<CartItem> findByTempCartIdAndProductId(String tempCartId, Long productId);
    
    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.tempCartId = :tempCartId")
    void deleteAllByTempCartId(String tempCartId);

    @Query("SELECT SUM(c.quantity * c.product.price) FROM CartItem c WHERE c.tempCartId = :tempCartId")
    BigDecimal calculateCartTotalByTempCartId(String tempCartId);
    
    @Modifying
    @Query("UPDATE CartItem c SET c.user = :userId, c.tempCartId = null WHERE c.tempCartId = :tempCartId")
    void mergeCart(Long userId, String tempCartId);

    
}