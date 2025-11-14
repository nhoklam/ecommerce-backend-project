package com.nhom13.ecommerce.service;

import com.nhom13.ecommerce.dto.ProductDTO;
import com.nhom13.ecommerce.dto.WishlistDTO;
import com.nhom13.ecommerce.entity.Product;
import com.nhom13.ecommerce.entity.User;
import com.nhom13.ecommerce.entity.WishlistItem;
import com.nhom13.ecommerce.exception.BadRequestException;
import com.nhom13.ecommerce.exception.ResourceNotFoundException;
import com.nhom13.ecommerce.repository.ProductRepository;
import com.nhom13.ecommerce.repository.UserRepository;
import com.nhom13.ecommerce.repository.WishlistItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductService productService; // Dùng để convertToDTO

    @Transactional(readOnly = true)
    public WishlistDTO getWishlist(Long userId) {
        List<WishlistItem> items = wishlistItemRepository.findByUserIdOrderByCreatedAtDesc(userId);

        List<ProductDTO> products = items.stream()
                .map(WishlistItem::getProduct)
                .map(productService::convertToDTO) // Tái sử dụng mapper của ProductService
                .collect(Collectors.toList());

        WishlistDTO dto = new WishlistDTO();
        dto.setProducts(products);
        dto.setCount(products.size());
        return dto;
    }

    public void addToWishlist(Long userId, Long productId) {
        if (wishlistItemRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new BadRequestException("Product is already in wishlist");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        WishlistItem item = new WishlistItem(user, product);
        wishlistItemRepository.save(item);
    }

    public void removeFromWishlist(Long userId, Long productId) {
        WishlistItem item = wishlistItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in wishlist"));

        wishlistItemRepository.delete(item);
    }

    @Transactional(readOnly = true)
    public int getWishlistCount(Long userId) {
        return wishlistItemRepository.countByUserId(userId);
    }
}
