package com.nhom13.ecommerce.service;

import com.nhom13.ecommerce.dto.RegisterRequest;
import com.nhom13.ecommerce.dto.UserDTO;
import com.nhom13.ecommerce.dto.UserProfileUpdateDTO;
import com.nhom13.ecommerce.entity.Role;
import com.nhom13.ecommerce.entity.User;
import com.nhom13.ecommerce.exception.BadRequestException;
import com.nhom13.ecommerce.exception.ResourceNotFoundException;
import com.nhom13.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

// [MỚI] Imports
import com.nhom13.ecommerce.entity.PasswordResetToken;
import com.nhom13.ecommerce.repository.PasswordResetTokenRepository;
import lombok.extern.slf4j.Slf4j;
// import org.springframework.transaction.annotation.Transactional; // [ĐÃ XÓA] - Bị trùng lặp
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j // [MỚI]
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // [MỚI] Injects
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    
    public UserDTO createUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }
        
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setRole(Role.CUSTOMER);
        user.setIsActive(true);
        
        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }
    
    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
          .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return convertToDTO(user);
    }
    
    @Transactional(readOnly = true)
    public UserDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
          .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return convertToDTO(user);
    }
    
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
          .map(this::convertToDTO)
          .collect(Collectors.toList());
    }
    
    /**
     * Phương thức này dành cho Admin, cho phép cập nhật cả 'role'.
     */
    public UserDTO updateUser(Long id, UserDTO userDTO) {
        User user = userRepository.findById(id)
          .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setPhone(userDTO.getPhone());
        if (userDTO.getRole()!= null) {
            user.setRole(userDTO.getRole());
        }

        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    /**
     * Phương thức an toàn cho người dùng tự cập nhật thông tin.
     * Thay đổi kiểu tham số thứ hai thành UserProfileUpdateDTO để khớp với Controller.
     */
    public UserDTO updateUserProfile(String email, UserProfileUpdateDTO profileDTO) {
        User user = userRepository.findByEmail(email)
          .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        // Bây giờ đoạn code này sẽ hoạt động chính xác
        user.setFirstName(profileDTO.getFirstName());
        user.setLastName(profileDTO.getLastName());
        user.setPhone(profileDTO.getPhone());
        // Không cập nhật role, email, hay isActive tại đây để đảm bảo an toàn.

        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }
    
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
          .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setIsActive(false);
        userRepository.save(user);
    }

    // [MỚI] Logic Quên mật khẩu
   @Transactional
    public void createPasswordResetToken(String email) {
        User user = userRepository.findByEmail(email)
          .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        
        // SỬA LỖI LOGIC:
        // 1. Tìm tất cả các token cũ của user
        List<PasswordResetToken> oldTokens = tokenRepository.findByUser(user);
        
        // 2. Xóa các token cũ đó một cách tường minh
        if (!oldTokens.isEmpty()) {
            tokenRepository.deleteAll(oldTokens);
        }

        // 3. Tạo token mới (logic này giữ nguyên)
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(1);
        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiryDate);
        
        tokenRepository.save(resetToken);
        
        // 4. Gửi email
        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }
    // [MỚI] Logic Đặt lại mật khẩu
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
          .orElseThrow(() -> new BadRequestException("Invalid or expired password reset token"));

        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            throw new BadRequestException("Invalid or expired password reset token");
        }

        User user = resetToken.getUser();
        
        // Cập nhật mật khẩu
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Xóa token sau khi sử dụng
        tokenRepository.delete(resetToken);
        log.info("Password has been reset for user: {}", user.getEmail());
    }

    // [MỚI] Logic Đổi mật khẩu
    @Transactional
    public void changePassword(String email, String oldPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
          .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        // Kiểm tra mật khẩu cũ
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BadRequestException("Incorrect old password");
        }

        // Cập nhật mật khẩu mới
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed successfully for user: {}", email);
    }
    
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole());
        dto.setIsActive(user.getIsActive());
        return dto;
    }
}