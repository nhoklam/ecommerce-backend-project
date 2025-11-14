package com.nhom13.ecommerce.repository;

import com.nhom13.ecommerce.entity.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Long> {
    // Các phương thức truy vấn cho khuyến mãi sẽ được thêm vào đây khi cần
}