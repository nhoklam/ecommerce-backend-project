package com.nhom13.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "promotion_banners")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PromotionBanner extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "target_url", length = 500)
    private String targetUrl; // Link khi click vào banner

    @Column(nullable = false)
    private Boolean isActive = false;
}