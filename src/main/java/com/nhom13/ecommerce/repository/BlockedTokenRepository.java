package com.nhom13.ecommerce.repository;

import com.nhom13.ecommerce.entity.BlockedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface BlockedTokenRepository extends JpaRepository<BlockedToken, Long> {
    boolean existsByToken(String token);
    void deleteAllByExpiryDateBefore(LocalDateTime now);
}