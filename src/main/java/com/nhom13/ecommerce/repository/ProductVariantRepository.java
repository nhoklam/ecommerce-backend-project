package com.nhom13.ecommerce.repository;

import com.nhom13.ecommerce.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional; // [QUAN TRỌNG] Thêm import này

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    
    // Phương thức này đã có
    boolean existsBySku(String sku);

    // [THÊM DÒNG NÀY] Phương thức còn thiếu để phục vụ logic update
    Optional<ProductVariant> findBySku(String sku);
}