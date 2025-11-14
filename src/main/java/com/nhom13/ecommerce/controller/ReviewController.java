package com.nhom13.ecommerce.controller;

import com.nhom13.ecommerce.dto.ReviewDTO;
import com.nhom13.ecommerce.dto.UserDTO;
import com.nhom13.ecommerce.service.ReviewService;
import com.nhom13.ecommerce.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
// Import mới
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserService userService;

    private Long getUserId(Authentication authentication) {
        UserDTO user = userService.getUserByEmail(authentication.getName());
        return user.getId();
    }

    // [THAY ĐỔI] Toàn bộ phương thức addReview
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ReviewDTO> addReview(
            @Valid @ModelAttribute ReviewDTO dto, // Dùng @ModelAttribute
            @RequestParam(value = "image", required = false) MultipartFile image, // Nhận tệp
            Authentication authentication) {

        // Truyền DTO và tệp ảnh vào service
        ReviewDTO review = reviewService.addReview(getUserId(authentication), dto, image);
        return new ResponseEntity<>(review, HttpStatus.CREATED);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<ReviewDTO>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) Integer rating) { // thêm rating

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<ReviewDTO> reviews;
        if (rating != null) {
            reviews = reviewService.getReviewsForProductByRating(productId, rating, pageable);
        } else {
            reviews = reviewService.getReviewsForProduct(productId, pageable);
        }
        return ResponseEntity.ok(reviews);
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId, Authentication authentication) {
        reviewService.deleteReview(reviewId, getUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/product/{productId}/stats")
    public ResponseEntity<Map<Integer, Long>> getRatingStats(@PathVariable Long productId) {
        Map<Integer, Long> stats = reviewService.getRatingStats(productId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/product/{productId}/average")
    public ResponseEntity<Double> getAverageRating(@PathVariable Long productId) {
        Double avg = reviewService.getAverageRating(productId);
        return ResponseEntity.ok(avg);
    }

}
