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
import com.nhom13.ecommerce.exception.BadRequestException;


import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CartController {

    private final CartService cartService;
    private final UserService userService;

    // ----------------- ADD TO CART -----------------
    @PostMapping("/add")
    public ResponseEntity<CartItemDTO> addToCart(
            @RequestParam(name = "productVariantId", required = false) Long productVariantId,
            @RequestParam(name = "productId", required = false) Long productId, // nhận cũ
            @RequestParam(defaultValue = "1") Integer quantity,
            Authentication authentication) {

        // Nếu frontend gửi productId, dùng nó như productVariantId
        if (productVariantId == null && productId != null) {
            productVariantId = productId;
        }

        if (productVariantId == null) {
            throw new BadRequestException("Missing productVariantId");
        }

        UserDTO user = userService.getUserByEmail(authentication.getName());
        CartItemDTO cartItem = cartService.addToCart(user.getId(), productVariantId, quantity);
        return new ResponseEntity<>(cartItem, HttpStatus.CREATED);
    }

    // ----------------- GET CART ITEMS -----------------
    @GetMapping
    public ResponseEntity<List<CartItemDTO>> getCartItems(Authentication authentication) {
        UserDTO user = userService.getUserByEmail(authentication.getName());
        List<CartItemDTO> cartItems = cartService.getCartItems(user.getId());
        return ResponseEntity.ok(cartItems);
    }

    // ----------------- GET CART TOTAL -----------------
    @GetMapping("/total")
    public ResponseEntity<BigDecimal> getCartTotal(Authentication authentication) {
        UserDTO user = userService.getUserByEmail(authentication.getName());
        BigDecimal total = cartService.getCartTotal(user.getId());
        return ResponseEntity.ok(total);
    }

    // ----------------- UPDATE CART ITEM -----------------
    @PutMapping("/{cartItemId}")
    public ResponseEntity<CartItemDTO> updateCartItem(
            @PathVariable Long cartItemId,
            @RequestParam Integer quantity,
            Authentication authentication) {

        UserDTO user = userService.getUserByEmail(authentication.getName());
        CartItemDTO updatedItem = cartService.updateCartItem(user.getId(), cartItemId, quantity);
        return ResponseEntity.ok(updatedItem);
    }

    // ----------------- REMOVE FROM CART -----------------
    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<Void> removeFromCart(
            @PathVariable Long cartItemId,
            Authentication authentication) {

        UserDTO user = userService.getUserByEmail(authentication.getName());
        cartService.removeFromCart(user.getId(), cartItemId);
        return ResponseEntity.noContent().build();
    }

    // ----------------- CLEAR CART -----------------
    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        UserDTO user = userService.getUserByEmail(authentication.getName());
        cartService.clearCart(user.getId());
        return ResponseEntity.noContent().build();
    }
}
