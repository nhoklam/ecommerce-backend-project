package com.nhom13.ecommerce.repository;

import com.nhom13.ecommerce.entity.PaymentStatus;
import com.nhom13.ecommerce.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    // Tìm giao dịch cho một đơn hàng theo trạng thái (PENDING, SUCCESSFUL, FAILED)
    Optional<PaymentTransaction> findByOrderIdAndStatus(Long orderId, PaymentStatus status);

    // (Đã xóa phương thức trùng lặp)
}