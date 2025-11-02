package com.nhom13.ecommerce.repository;

import com.nhom13.ecommerce.entity.RefundRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {

    /**
     * Lấy tất cả các yêu cầu trả hàng của một người dùng cụ thể.
     * Spring Data JPA tự động tạo truy vấn từ tên phương thức.[10, 11]
     */
    List<RefundRequest> findByUserId(Long userId);

    /**
     * Tìm tất cả các yêu cầu theo ID đơn hàng (để kiểm tra nghiệp vụ nếu cần).
     */
    List<RefundRequest> findByOrderId(Long orderId);

    /**
     * Ghi đè phương thức findAll để hỗ trợ phân trang cho Admin.
     */
    Page<RefundRequest> findAll(Pageable pageable);
}