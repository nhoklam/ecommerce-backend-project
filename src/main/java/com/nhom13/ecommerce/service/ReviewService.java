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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;

    public ReviewDTO addReview(Long userId, ReviewDTO dto) {
        User user = userRepository.findById(userId)
          .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Product product = productRepository.findById(dto.getProductId())
          .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // 1. Kiểm tra xem đã review chưa
        if (reviewRepository.existsByUserIdAndProductId(userId, dto.getProductId())) {
            throw new BadRequestException("You have already reviewed this product");
        }

        // 2. Kiểm tra xem đã mua (và nhận) hàng chưa
        if (!reviewRepository.didUserPurchaseProduct(userId, dto.getProductId())) {
            throw new BadRequestException("You must purchase and receive the product to review it");
        }

        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());
        review.setImageUrl(dto.getImageUrl()); // Giả định URL ảnh được gửi lên (hoặc xử lý upload file nếu cần)

        Review savedReview = reviewRepository.save(review);
        
        // Cập nhật rating trung bình cho sản phẩm
        updateProductAverageRating(dto.getProductId());

        return convertToDTO(savedReview);
    }

    @Transactional(readOnly = true)
    public Page<ReviewDTO> getReviewsForProduct(Long productId, Pageable pageable) {
        return reviewRepository.findByProductId(productId, pageable)
          .map(this::convertToDTO);
    }

    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
          .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getUser().getId().equals(userId)) {
            throw new BadRequestException("You can only delete your own reviews");
        }

        Long productId = review.getProduct().getId();
        
        // Bỏ comment để sử dụng service
        if (review.getImageUrl()!= null &&!review.getImageUrl().isEmpty()) {
            // Giả định imageUrl là tên file, không phải URL đầy đủ
             fileStorageService.deleteFile(review.getImageUrl());
        }

        reviewRepository.delete(review);
        
        // Cập nhật lại rating
        updateProductAverageRating(productId);
    }

    // Helper cập nhật Product
    private void updateProductAverageRating(Long productId) {
        Product product = productRepository.findById(productId)
          .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        
        Double averageRating = reviewRepository.calculateAverageRating(productId);
        long reviewCount = reviewRepository.countByProductId(productId); // Cần thêm method này vào Repo

        product.setAverageRating(averageRating!= null? averageRating : 0.0);
        product.setReviewCount((int) reviewCount);
        
        productRepository.save(product);
    }

    // Helper Mappers
    private ReviewDTO convertToDTO(Review entity) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(entity.getId());
        dto.setProductId(entity.getProduct().getId());
        dto.setUserId(entity.getUser().getId());
        dto.setUserFullName(entity.getUser().getFirstName() + " " + entity.getUser().getLastName());
        dto.setRating(entity.getRating());
        dto.setComment(entity.getComment());
        dto.setImageUrl(entity.getImageUrl());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}