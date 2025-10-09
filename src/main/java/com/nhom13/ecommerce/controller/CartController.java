package com.nhom13.ecommerce.controller;

import com.nhom13.ecommerce.dto.CartItemDTO;
import com.nhom13.ecommerce.dto.UserDTO;
import com.nhom13.ecommerce.service.CartService;
import com.nhom13.ecommerce.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CartController {
    
    private final CartService cartService;
    private final UserService userService;
    
    @PostMapping("/add")
    public ResponseEntity<CartItemDTO> addToCart(
            @RequestParam Long productId,
            @RequestParam Integer quantity,
            Authentication authentication) {
        
        UserDTO user = userService.getUserByEmail(authentication.getName());
        CartItemDTO cartItem = cartService.addToCart(user.getId(), productId, quantity);
        return new ResponseEntity<>(cartItem, HttpStatus.CREATED);
    }
    
    @GetMapping
    public ResponseEntity<List<CartItemDTO>> getCartItems(Authentication authentication) {
        UserDTO user = userService.getUserByEmail(authentication.getName());
        List<CartItemDTO> cartItems = cartService.getCartItems(user.getId());
        return ResponseEntity.ok(cartItems);
    }
    
    @GetMapping("/total")
    public ResponseEntity<BigDecimal> getCartTotal(Authentication authentication) {
        UserDTO user = userService.getUserByEmail(authentication.getName());
        BigDecimal total = cartService.getCartTotal(user.getId());
        return ResponseEntity.ok(total);
    }
    
    @PutMapping("/{cartItemId}")
    public ResponseEntity<CartItemDTO> updateCartItem(
            @PathVariable Long cartItemId,
            @RequestParam Integer quantity,
            Authentication authentication) {
        
        UserDTO user = userService.getUserByEmail(authentication.getName());
        CartItemDTO updatedItem = cartService.updateCartItem(user.getId(), cartItemId, quantity);
        return ResponseEntity.ok(updatedItem);
    }
    
    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<Void> removeFromCart(@PathVariable Long cartItemId, Authentication authentication) {
        UserDTO user = userService.getUserByEmail(authentication.getName());
        cartService.removeFromCart(user.getId(), cartItemId);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        UserDTO user = userService.getUserByEmail(authentication.getName());
        cartService.clearCart(user.getId());
        return ResponseEntity.noContent().build();
    }
}