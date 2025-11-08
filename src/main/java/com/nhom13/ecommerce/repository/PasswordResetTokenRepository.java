package com.nhom13.ecommerce.repository;

import com.nhom13.ecommerce.entity.PasswordResetToken;
import com.nhom13.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; // <--- THÊM IMPORT NÀY
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    
    // SỬA Ở ĐÂY: Thay thế deleteByUser bằng findByUser
    List<PasswordResetToken> findByUser(User user); 

    void deleteAllByExpiryDateBefore(java.time.LocalDateTime now);
}