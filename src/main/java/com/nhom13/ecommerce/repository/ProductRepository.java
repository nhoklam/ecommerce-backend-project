package com.nhom13.ecommerce.repository;

import com.nhom13.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    @Query("SELECT p FROM Product p WHERE p.isActive = true")
    Page<Product> findAllActiveProducts(Pageable pageable);
    
    List<Product> findByCategoryIdAndIsActiveTrue(Long categoryId);
    
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND " +
           "(:name IS NULL OR p.name LIKE %:name%) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Product> findProductsWithFilters(
        @Param("name") String name,
        @Param("categoryId") Long categoryId,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        Pageable pageable
    );
    
    List<Product> findByNameContainingIgnoreCaseAndIsActiveTrue(String name);
    
    

    // Thêm phương thức bị thiếu
    List<Product> findByIsFeaturedTrueAndIsActiveTrue();
}