package com.nhom13.ecommerce.controller;

import com.nhom13.ecommerce.dto.UserDTO;
import com.nhom13.ecommerce.dto.WishlistDTO;
import com.nhom13.ecommerce.service.UserService;
import com.nhom13.ecommerce.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WishlistController {

    private final WishlistService wishlistService;
    private final UserService userService;

    private Long getUserId(Authentication authentication) {
        UserDTO user = userService.getUserByEmail(authentication.getName());
        return user.getId();
    }

    @GetMapping
    public ResponseEntity<WishlistDTO> getMyWishlist(Authentication authentication) {
        WishlistDTO wishlist = wishlistService.getWishlist(getUserId(authentication));
        return ResponseEntity.ok(wishlist);
    }

    @PostMapping("/{productId}")
    public ResponseEntity<Map<String, String>> addToWishlist(
            @PathVariable Long productId,
            Authentication authentication) {

        wishlistService.addToWishlist(getUserId(authentication), productId);
        return new ResponseEntity<>(Map.of("message", "Product added to wishlist"), HttpStatus.CREATED);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> removeFromWishlist(
            @PathVariable Long productId,
            Authentication authentication) {

        wishlistService.removeFromWishlist(getUserId(authentication), productId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Integer>> getWishlistCount(Authentication authentication) {
        UserDTO user = userService.getUserByEmail(authentication.getName());
        int count = wishlistService.getWishlistCount(user.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }
}
