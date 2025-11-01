package com.nhom13.ecommerce.controller;

import com.nhom13.ecommerce.dto.PromotionBannerDTO;
import com.nhom13.ecommerce.service.PromotionBannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PromotionBannerController {

    private final PromotionBannerService bannerService;

    // API Public cho client
    @GetMapping("/active")
    public ResponseEntity<List<PromotionBannerDTO>> getActiveBanners() {
        return ResponseEntity.ok(bannerService.getActiveBanners());
    }

    // APIs cho Admin
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PromotionBannerDTO>> getAllBanners() {
        return ResponseEntity.ok(bannerService.getAllBanners());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromotionBannerDTO> createBanner(@Valid @RequestBody PromotionBannerDTO dto) {
        PromotionBannerDTO createdBanner = bannerService.createBanner(dto);
        return new ResponseEntity<>(createdBanner, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromotionBannerDTO> updateBanner(@PathVariable Long id, @Valid @RequestBody PromotionBannerDTO dto) {
        return ResponseEntity.ok(bannerService.updateBanner(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBanner(@PathVariable Long id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.noContent().build();
    }
}