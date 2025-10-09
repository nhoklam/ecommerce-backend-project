package com.nhom13.ecommerce.service;

import com.nhom13.ecommerce.dto.CartItemDTO;
import com.nhom13.ecommerce.entity.CartItem;
import com.nhom13.ecommerce.entity.Product;
import com.nhom13.ecommerce.entity.User;
import com.nhom13.ecommerce.exception.BadRequestException;
import com.nhom13.ecommerce.exception.ResourceNotFoundException;
import com.nhom13.ecommerce.repository.CartItemRepository;
import com.nhom13.ecommerce.repository.ProductRepository;
import com.nhom13.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {
    
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    
    public CartItemDTO addToCart(Long userId, Long productId, Integer quantity) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        
        if (!product.getIsActive()) {
            throw new BadRequestException("Product is not available");
        }
        
        if (product.getStockQuantity() < quantity) {
            throw new BadRequestException("Insufficient stock. Available: " + product.getStockQuantity());
        }
        
        Optional<CartItem> existingItem = cartItemRepository.findByUserIdAndProductId(userId, productId);
        
        CartItem cartItem;
        if (existingItem.isPresent()) {
            cartItem = existingItem.get();
            int newQuantity = cartItem.getQuantity() + quantity;
            
            if (product.getStockQuantity() < newQuantity) {
                throw new BadRequestException("Insufficient stock. Available: " + product.getStockQuantity());
            }
            
            cartItem.setQuantity(newQuantity);
        } else {
            cartItem = new CartItem();
            cartItem.setUser(user);
            cartItem.setProduct(product);
            cartItem.setQuantity(quantity);
        }
        
        CartItem savedItem = cartItemRepository.save(cartItem);
        return convertToDTO(savedItem);
    }
    
    public CartItemDTO updateCartItem(Long userId, Long cartItemId, Integer quantity) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
            .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));
        
        if (!cartItem.getUser().getId().equals(userId)) {
            throw new BadRequestException("Cart item does not belong to user");
        }
        
        if (cartItem.getProduct().getStockQuantity() < quantity) {
            throw new BadRequestException("Insufficient stock. Available: " + cartItem.getProduct().getStockQuantity());
        }
        
        cartItem.setQuantity(quantity);
        CartItem updatedItem = cartItemRepository.save(cartItem);
        return convertToDTO(updatedItem);
    }
    
    public void removeFromCart(Long userId, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
            .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));
        
        if (!cartItem.getUser().getId().equals(userId)) {
            throw new BadRequestException("Cart item does not belong to user");
        }
        
        cartItemRepository.delete(cartItem);
    }
    
    @Transactional(readOnly = true)
    public List<CartItemDTO> getCartItems(Long userId) {
        return cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public BigDecimal getCartTotal(Long userId) {
        BigDecimal total = cartItemRepository.calculateCartTotal(userId);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    public void clearCart(Long userId) {
        cartItemRepository.deleteAllByUserId(userId);
    }
    
    private CartItemDTO convertToDTO(CartItem cartItem) {
        CartItemDTO dto = new CartItemDTO();
        dto.setId(cartItem.getId());
        dto.setProductId(cartItem.getProduct().getId());
        dto.setProductName(cartItem.getProduct().getName());
        dto.setUnitPrice(cartItem.getProduct().getPrice());
        dto.setQuantity(cartItem.getQuantity());
        dto.setTotalPrice(cartItem.getProduct().getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        dto.setImageUrl(cartItem.getProduct().getImageUrl());
        return dto;
    }
}