package com.nhom13.ecommerce.controller;


import com.nhom13.ecommerce.dto.UserDTO;
import com.nhom13.ecommerce.dto.UserProfileUpdateDTO; 
import com.nhom13.ecommerce.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

// [MỚI] Imports
import com.nhom13.ecommerce.dto.ChangePasswordRequest;
import com.nhom13.ecommerce.service.SearchHistoryService;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {
    
    private final UserService userService;

    // [MỚI] Inject
    private final SearchHistoryService searchHistoryService;

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        UserDTO user = userService.getUserByEmail(authentication.getName());
        return ResponseEntity.ok(user);
    }
    
    /**
     * Endpoint cho người dùng tự cập nhật thông tin cá nhân.
     * Sử dụng UserProfileUpdateDTO để validation chính xác những gì client gửi lên.
     * Điều này sẽ khắc phục lỗi 400 Bad Request.
     */
    @PutMapping("/me")
    public ResponseEntity<UserDTO> updateCurrentUser(@Valid @RequestBody UserProfileUpdateDTO profileDTO, Authentication authentication) {
        // Gọi đến phương thức service đã được cập nhật để nhận DTO mới
        UserDTO updatedUser = userService.updateUserProfile(authentication.getName(), profileDTO);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * [MỚI] Endpoint cho người dùng thay đổi địa chỉ email của họ.
     */
    
    // [MỚI] API Đổi mật khẩu
    @PostMapping("/me/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        
        userService.changePassword(
            authentication.getName(), 
            request.getOldPassword(), 
            request.getNewPassword()
        );
        
        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }

    // [MỚI] API Lịch sử tìm kiếm
    @GetMapping("/me/search-history")
    public ResponseEntity<List<String>> getSearchHistory(Authentication authentication) {
        UserDTO user = userService.getUserByEmail(authentication.getName());
        List<String> history = searchHistoryService.getSearchHistory(user.getId(), 10); // Lấy 10 query gần nhất
        return ResponseEntity.ok(history);
    }
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint này dành cho Admin, giữ nguyên để admin có thể cập nhật cả vai trò của người dùng.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @Valid @RequestBody UserDTO userDTO) {
        UserDTO updatedUser = userService.updateUser(id, userDTO);
        return ResponseEntity.ok(updatedUser);
    }
}