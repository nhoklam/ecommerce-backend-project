package com.nhom13.ecommerce.service;

import com.nhom13.ecommerce.entity.BlockedToken;
import com.nhom13.ecommerce.repository.BlockedTokenRepository;
import com.nhom13.ecommerce.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TokenBlocklistService {

    private final BlockedTokenRepository blockedTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public void blockToken(String token) {
        if (token!= null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        if (jwtTokenProvider.validateToken(token)) {
            java.util.Date expiryDateUtil = jwtTokenProvider.getExpirationDateFromToken(token);
            LocalDateTime expiryDate = expiryDateUtil.toInstant()
              .atZone(java.time.ZoneId.systemDefault())
              .toLocalDateTime();

            BlockedToken blockedToken = new BlockedToken(token, expiryDate);
            blockedTokenRepository.save(blockedToken);
        }
    }

    public boolean isTokenBlocked(String token) {
        return blockedTokenRepository.existsByToken(token);
    }

    // Chạy job dọn dẹp token hết hạn mỗi ngày lúc 3 giờ sáng
    //
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpiredTokens() {
        blockedTokenRepository.deleteAllByExpiryDateBefore(LocalDateTime.now());
    }
}