package com.nhom13.ecommerce.repository;

import com.nhom13.ecommerce.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // Thêm import Param
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    
    // === CÁC PHƯƠNG THỨC CHO USER ĐÃ ĐĂNG NHẬP ===

    List<CartItem> findByUserIdOrderByCreatedAtDesc(Long userId);

    // [SỬA] Thay thế findByUserIdAndProductId bằng phương thức mới
    Optional<CartItem> findByUserIdAndProductVariantId(Long userId, Long productVariantId);
    
    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
    
    // [SỬA] Cập nhật câu lệnh JPQL để tính tổng tiền dựa trên giá của variant
    @Query("SELECT SUM(c.quantity * COALESCE(c.productVariant.price, c.productVariant.product.price)) FROM CartItem c WHERE c.user.id = :userId")
    BigDecimal calculateCartTotal(@Param("userId") Long userId);

    
    // === CÁC PHƯƠง THỨC CHO GUEST (GIỎ HÀNG TẠM) ===

    List<CartItem> findByTempCartIdOrderByCreatedAtDesc(String tempCartId);

    // [SỬA] Thay thế findByTempCartIdAndProductId bằng phương thức mới
    Optional<CartItem> findByTempCartIdAndProductVariantId(String tempCartId, Long productVariantId);
    
    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.tempCartId = :tempCartId")
    void deleteAllByTempCartId(@Param("tempCartId") String tempCartId);

    // [SỬA] Cập nhật câu lệnh JPQL để tính tổng tiền dựa trên giá của variant
    @Query("SELECT SUM(c.quantity * COALESCE(c.productVariant.price, c.productVariant.product.price)) FROM CartItem c WHERE c.tempCartId = :tempCartId")
    BigDecimal calculateCartTotalByTempCartId(@Param("tempCartId") String tempCartId);
    
    @Modifying
    @Query("UPDATE CartItem c SET c.user.id = :userId, c.tempCartId = null WHERE c.tempCartId = :tempCartId")
    void mergeCart(@Param("userId") Long userId, @Param("tempCartId") String tempCartId);
}