package com.nhom13.ecommerce.repository;

import com.nhom13.ecommerce.entity.PromotionBanner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionBannerRepository extends JpaRepository<PromotionBanner, Long> {
    List<PromotionBanner> findAllByIsActiveTrueOrderByCreatedAtDesc();
}