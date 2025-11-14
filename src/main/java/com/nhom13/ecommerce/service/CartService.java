package com.nhom13.ecommerce.service;

import com.nhom13.ecommerce.dto.CartItemDTO;
import com.nhom13.ecommerce.entity.CartItem;
import com.nhom13.ecommerce.entity.Product;
import com.nhom13.ecommerce.entity.ProductVariant;
import com.nhom13.ecommerce.entity.User;
import com.nhom13.ecommerce.exception.BadRequestException;
import com.nhom13.ecommerce.exception.ResourceNotFoundException;
import com.nhom13.ecommerce.repository.CartItemRepository;
import com.nhom13.ecommerce.repository.ProductVariantRepository;
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
    private final ProductVariantRepository productVariantRepository; // Sửa: Dùng ProductVariantRepository
    private final UserRepository userRepository;
    
    // Sửa: Nhận vào productVariantId thay vì productId
    public CartItemDTO addToCart(Long userId, Long productVariantId, Integer quantity) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        ProductVariant variant = productVariantRepository.findById(productVariantId)
            .orElseThrow(() -> new ResourceNotFoundException("Product Variant not found with id: " + productVariantId));
        
        if (!variant.getProduct().getIsActive()) {
            throw new BadRequestException("Product is not available");
        }
        
        // Sửa: Kiểm tra kho của biến thể
        if (variant.getStockQuantity() < quantity) {
            throw new BadRequestException("Insufficient stock. Available: " + variant.getStockQuantity());
        }
        
        // Sửa: Tìm CartItem theo userId và productVariantId
        Optional<CartItem> existingItem = cartItemRepository.findByUserIdAndProductVariantId(userId, productVariantId);
        
        CartItem cartItem;
        if (existingItem.isPresent()) {
            cartItem = existingItem.get();
            int newQuantity = cartItem.getQuantity() + quantity;
            
            // Sửa: Kiểm tra lại kho của biến thể
            if (variant.getStockQuantity() < newQuantity) {
                throw new BadRequestException("Insufficient stock. Available: " + variant.getStockQuantity());
            }
            
            cartItem.setQuantity(newQuantity);
        } else {
            cartItem = new CartItem();
            cartItem.setUser(user);
            cartItem.setProductVariant(variant); // Sửa: Gán productVariant
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
        
        // Sửa: Kiểm tra kho của biến thể
        if (cartItem.getProductVariant().getStockQuantity() < quantity) {
            throw new BadRequestException("Insufficient stock. Available: " + cartItem.getProductVariant().getStockQuantity());
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
        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return cartItems.stream()
            .map(this::calculateItemTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private BigDecimal calculateItemTotal(CartItem cartItem) {
        ProductVariant variant = cartItem.getProductVariant();
        BigDecimal price = variant.getPrice() != null ? variant.getPrice() : variant.getProduct().getPrice();
        return price.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
    }

    public void clearCart(Long userId) {
        cartItemRepository.deleteAllByUserId(userId);
    }
    
    private CartItemDTO convertToDTO(CartItem cartItem) {
        CartItemDTO dto = new CartItemDTO();
        ProductVariant variant = cartItem.getProductVariant();
        Product product = variant.getProduct();

        dto.setId(cartItem.getId());
        dto.setProductId(product.getId());
        dto.setProductVariantId(variant.getId());
        dto.setProductName(product.getName());
        dto.setQuantity(cartItem.getQuantity());

        // Ưu tiên thông tin của variant, nếu không có thì lấy của product
        BigDecimal unitPrice = variant.getPrice() != null ? variant.getPrice() : product.getPrice();
        String imageUrl = variant.getImageUrl() != null ? variant.getImageUrl() : product.getImageUrl();
        dto.setColor(variant.getColor());
        dto.setSize(variant.getProductSize());

        dto.setUnitPrice(unitPrice);
        dto.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        dto.setImageUrl(imageUrl);
        return dto;
    }
}