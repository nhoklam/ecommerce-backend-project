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
import org.springframework.web.multipart.MultipartFile; // Import cho file upload

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService; // Inject FileStorageService

    /**
     * Thêm đánh giá mới, có hỗ trợ upload ảnh.
     *
     * @param userId ID của người dùng
     * @param dto DTO chứa thông tin (productId, rating, comment)
     * @param imageFile Tệp ảnh (có thể là null)
     * @return ReviewDTO đã được tạo
     */
    // [THAY ĐỔI] Thêm tham số MultipartFile imageFile
    public ReviewDTO addReview(Long userId, ReviewDTO dto, MultipartFile imageFile) {
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

        // 3. [MỚI] Xử lý upload file ảnh
        String fileName = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            // Lưu file vào thư mục 'uploads' và lấy tên file duy nhất
            fileName = fileStorageService.storeFile(imageFile);
        }

        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());
        review.setImageUrl(fileName); // [MỚI] Gán tên file đã lưu (hoặc null)

        Review savedReview = reviewRepository.save(review);
        
        // 4. Cập nhật rating trung bình cho sản phẩm
        updateProductAverageRating(dto.getProductId());

        return convertToDTO(savedReview);
    }

    /**
     * Lấy danh sách đánh giá cho một sản phẩm (có phân trang).
     */
    @Transactional(readOnly = true)
    public Page<ReviewDTO> getReviewsForProduct(Long productId, Pageable pageable) {
        return reviewRepository.findByProductId(productId, pageable)
          .map(this::convertToDTO);
    }

    /**
     * Xóa một đánh giá (chỉ chủ sở hữu mới được xóa).
     */
    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
          .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        
        if (!review.getUser().getId().equals(userId)) {
            throw new BadRequestException("You can only delete your own reviews");
        }

        Long productId = review.getProduct().getId();

        // [MỚI] Xóa tệp ảnh liên quan (nếu có) khỏi thư mục 'uploads'
        if (review.getImageUrl() != null && !review.getImageUrl().isEmpty()) {
             fileStorageService.deleteFile(review.getImageUrl());
        }

        reviewRepository.delete(review);
        
        // Cập nhật lại rating sau khi xóa
        updateProductAverageRating(productId);
    }

    /**
     * Hàm helper để tính toán và cập nhật rating trung bình
     * và tổng số review cho một sản phẩm.
     */
    private void updateProductAverageRating(Long productId) {
        Product product = productRepository.findById(productId)
          .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        
        Double averageRating = reviewRepository.calculateAverageRating(productId);
        long reviewCount = reviewRepository.countByProductId(productId);

        product.setAverageRating(averageRating != null ? averageRating : 0.0);
        product.setReviewCount((int) reviewCount);
        
        productRepository.save(product);
    }

    /**
     * Hàm helper để chuyển đổi Entity (Review) sang DTO (ReviewDTO).
     */
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