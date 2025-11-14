package com.nhom13.ecommerce.service;

import com.nhom13.ecommerce.dto.ReviewDTO;
import com.nhom13.ecommerce.entity.Product;
import com.nhom13.ecommerce.entity.Review;
import com.nhom13.ecommerce.entity.User;
import com.nhom13.ecommerce.exception.BadRequestException;
import com.nhom13.ecommerce.exception.ResourceNotFoundException;
import com.nhom13.ecommerce.repository.ProductRepository;
import com.nhom13.ecommerce.repository.ReviewRepository;
import com.nhom13.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;

    // ADD REVIEW
    public ReviewDTO addReview(Long userId, ReviewDTO dto, MultipartFile imageFile) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (reviewRepository.existsByUserIdAndProductId(userId, dto.getProductId())) {
            throw new BadRequestException("You have already reviewed this product");
        }

        if (!reviewRepository.didUserPurchaseProduct(userId, dto.getProductId())) {
            throw new BadRequestException("You must purchase and receive the product to review it");
        }

        String fileName = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            fileName = fileStorageService.storeFile(imageFile);
        }

        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());
        review.setImageUrl(fileName);

        Review savedReview = reviewRepository.save(review);
        updateProductAverageRating(dto.getProductId());

        return convertToDTO(savedReview);
    }

    // GET REVIEWS FOR PRODUCT (FIXED: Use fetch join)
    @Transactional(readOnly = true)
    public Page<ReviewDTO> getReviewsForProduct(Long productId, Pageable pageable) {
        return reviewRepository.findByProductIdWithUser(productId, pageable)
                .map(this::convertToDTO);
    }

    // DELETE REVIEW
    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getUser().getId().equals(userId)) {
            throw new BadRequestException("You can only delete your own reviews");
        }

        Long productId = review.getProduct().getId();

        if (review.getImageUrl() != null && !review.getImageUrl().isEmpty()) {
            fileStorageService.deleteFile(review.getImageUrl());
        }

        reviewRepository.delete(review);
        updateProductAverageRating(productId);
    }

    // UPDATE PRODUCT RATING
    private void updateProductAverageRating(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Double averageRating = reviewRepository.calculateAverageRating(productId);
        long reviewCount = reviewRepository.countByProductId(productId);

        product.setAverageRating(averageRating != null ? averageRating : 0.0);
        product.setReviewCount((int) reviewCount);
        productRepository.save(product);
    }

    // CONVERT ENTITY TO DTO (SAFE: Null protection)
    private ReviewDTO convertToDTO(Review entity) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(entity.getId());
        dto.setProductId(entity.getProduct().getId());

        User user = entity.getUser();
        if (user != null) {
            dto.setUserId(user.getId());
            dto.setUserFullName(user.getFirstName() + " " + user.getLastName());
        } else {
            dto.setUserId(null);
            dto.setUserFullName("Khách");
        }

        dto.setRating(entity.getRating());
        dto.setComment(entity.getComment());
        dto.setImageUrl(entity.getImageUrl()); // ddbc4ccc-18a0-4bb7-a3b4-1bea8541f582.png
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    @Transactional(readOnly = true)
    public Page<ReviewDTO> getReviewsForProductByRating(Long productId, int rating, Pageable pageable) {
        return reviewRepository.findByProductIdAndRatingWithUser(productId, rating, pageable)
                .map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public Map<Integer, Long> getRatingStats(Long productId) {
        Map<Integer, Long> stats = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            stats.put(i, reviewRepository.countByProductIdAndRating(productId, i));
        }
        return stats;
    }
    public Double getAverageRating(Long productId) {
    return reviewRepository.calculateAverageRating(productId);
}


}
