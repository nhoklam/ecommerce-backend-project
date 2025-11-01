package com.nhom13.ecommerce.service;

import com.nhom13.ecommerce.entity.WishlistItem; 
import com.nhom13.ecommerce.repository.OrderRepository;
import com.nhom13.ecommerce.dto.ProductDTO;
import com.nhom13.ecommerce.dto.SearchResultDTO;
import com.nhom13.ecommerce.entity.Product;
import com.nhom13.ecommerce.entity.User;
import com.nhom13.ecommerce.entity.WishlistItem; //
import com.nhom13.ecommerce.repository.OrderRepository;
import com.nhom13.ecommerce.repository.ProductRepository;
import com.nhom13.ecommerce.repository.UserRepository;
import com.nhom13.ecommerce.repository.WishlistItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {
    
    private final ProductRepository productRepository;
    
    // [MỚI] Injects
    private final SearchHistoryService searchHistoryService;
    private final UserRepository userRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final OrderRepository orderRepository; // (Có thể dùng cho gợi ý nâng cao)
    private final ProductService productService; // Inject ProductService

    public SearchResultDTO searchProducts(
            String query,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String sortBy,
            String sortDirection,
            int page,
            int size) {
        
        // Khôi phục logic tạo Sort và Pageable
        Sort sort = createSort(sortBy, sortDirection);
        Pageable pageable = PageRequest.of(page, size, sort);

        // [MỚI] Lưu lịch sử tìm kiếm
        saveSearchHistory(query);

        Page<Product> productPage = productRepository.findProductsWithFilters(
            query, categoryId, minPrice, maxPrice, pageable);
        
        SearchResultDTO result = new SearchResultDTO();
        result.setProducts(productPage.getContent().stream()
          .map(productService::convertToDTO) // Sử dụng productService
          .collect(Collectors.toList()));
        result.setTotalElements(productPage.getTotalElements());
        result.setTotalPages(productPage.getTotalPages());
        result.setCurrentPage(page);
        result.setSize(size);
        
        return result;
    }

    // [MỚI] Helper
    @Transactional
    private void saveSearchHistory(String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication!= null && authentication.isAuthenticated() &&!authentication.getPrincipal().equals("anonymousUser")) {
            String email = authentication.getName();
            userRepository.findByEmail(email).ifPresent(user -> 
                searchHistoryService.addSearchQuery(user, query)
            );
        }
    }

    // Logic gợi ý (Phần 3.2)
    @Transactional(readOnly = true)
    public List<ProductDTO> getRecommendedProducts(Long productId, int limit) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication!= null && authentication.isAuthenticated() &&!authentication.getPrincipal().equals("anonymousUser")) {
            Long userId = userRepository.findByEmail(authentication.getName()).get().getId();
            
            // Lấy sản phẩm từ wishlist
            List<ProductDTO> wishlistProducts = wishlistItemRepository.findByUserIdOrderByCreatedAtDesc(userId)
             .stream()
             .map(WishlistItem::getProduct) // <-- Dòng này gây lỗi
             .map(productService::convertToDTO) // Sử dụng productService
             .filter(p ->!p.getId().equals(productId))
             .limit(limit)
             .collect(Collectors.toList());
            
            if(wishlistProducts.size() >= limit) {
                return wishlistProducts;
            }
        }
        
        // Logic cũ (fallback)
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return List.of();
        }
        Pageable pageable = PageRequest.of(0, limit);
        return productRepository.findByCategoryIdAndIsActiveTrue(product.getCategory().getId())
         .stream()
         .filter(p ->!p.getId().equals(productId))
         .limit(limit)
         .map(productService::convertToDTO) // Sử dụng productService
         .collect(Collectors.toList());
    }

    private Sort createSort(String sortBy, String sortDirection) {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") 
          ? Sort.Direction.DESC 
            : Sort.Direction.ASC;
        
        return Sort.by(direction, sortBy);
    }
    
    // Xóa phương thức private convertToDTO, vì đã dùng của ProductService
}