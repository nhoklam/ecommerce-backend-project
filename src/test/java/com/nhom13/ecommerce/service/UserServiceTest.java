package com.nhom13.ecommerce.service;

import com.nhom13.ecommerce.dto.RegisterRequest;
import com.nhom13.ecommerce.dto.UserDTO;
// === PHẦN BỔ SUNG MỚI ===
import com.nhom13.ecommerce.dto.UserProfileUpdateDTO;
import com.nhom13.ecommerce.entity.PasswordResetToken;
// === KẾT THÚC PHẦN BỔ SUNG ===
import com.nhom13.ecommerce.entity.Role;
import com.nhom13.ecommerce.entity.User;
import com.nhom13.ecommerce.exception.BadRequestException;
// === PHẦN BỔ SUNG MỚI ===
import com.nhom13.ecommerce.exception.ResourceNotFoundException;
import com.nhom13.ecommerce.repository.PasswordResetTokenRepository;
import com.nhom13.ecommerce.service.EmailService;
// === KẾT THÚC PHẦN BỔ SUNG ===
import com.nhom13.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

// === PHẦN BỔ SUNG MỚI ===
import java.time.LocalDateTime;
// === KẾT THÚC PHẦN BỔ SUNG ===
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    
    // === PHẦN BỔ SUNG MỚI ===
    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private EmailService emailService;
    // === KẾT THÚC PHẦN BỔ SUNG ===

    @InjectMocks
    private UserService userService;
    
    private RegisterRequest registerRequest;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setPhone("1234567890");
        
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPhone("1234567890");
        user.setRole(Role.CUSTOMER);
        user.setIsActive(true);
    }
    
    @Test
    void createUser_ShouldCreateUser_WhenValidRequest() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        // When
        UserDTO result = userService.createUser(registerRequest);
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(registerRequest.getEmail());
        assertThat(result.getFirstName()).isEqualTo(registerRequest.getFirstName());
        assertThat(result.getRole()).isEqualTo(Role.CUSTOMER);
        
        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(passwordEncoder).encode(registerRequest.getPassword());
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void createUser_ShouldThrowException_WhenEmailExists() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(true);
        // When & Then
        assertThatThrownBy(() -> userService.createUser(registerRequest))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Email already exists");
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void getUserByEmail_ShouldReturnUser_WhenUserExists() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        // When
        UserDTO result = userService.getUserByEmail("test@example.com");
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(user.getEmail());
        
        verify(userRepository).findByEmail("test@example.com");
    }

    // === PHẦN BỔ SUNG MỚI ===

    @Test
    void updateUserProfile_ShouldUpdateFields_WhenUserExists() {
        // Given
        UserProfileUpdateDTO dto = new UserProfileUpdateDTO();
        dto.setFirstName("Jane");
        dto.setLastName("Doer");
        dto.setPhone("000000000");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UserDTO result = userService.updateUserProfile("test@example.com", dto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("Jane");
        assertThat(result.getLastName()).isEqualTo("Doer");
        assertThat(result.getPhone()).isEqualTo("000000000");
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_ShouldUpdatePassword_WhenOldPasswordIsCorrect() {
        // Given
        String oldPass = "password123";
        String newPass = "newPassword456";
        String encodedNewPass = "encodedNewPassword";

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(oldPass, "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode(newPass)).thenReturn(encodedNewPass);

        // When
        userService.changePassword("test@example.com", oldPass, newPass);

        // Then
        verify(passwordEncoder).encode(newPass);
        verify(userRepository).save(user);
        assertThat(user.getPassword()).isEqualTo(encodedNewPass);
    }

    @Test
    void changePassword_ShouldThrowException_WhenOldPasswordIsIncorrect() {
        // Given
        String oldPass = "wrongPassword";
        String newPass = "newPassword456";

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(oldPass, "encodedPassword")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.changePassword("test@example.com", oldPass, newPass))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Incorrect old password");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createPasswordResetToken_ShouldCreateAndSendToken() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // When
        userService.createPasswordResetToken("test@example.com");

        // Then
        verify(tokenRepository).deleteAll(any()); // Xác minh token cũ đã bị xóa
        verify(tokenRepository).save(any(PasswordResetToken.class)); // Xác minh token mới đã được lưu
        verify(emailService).sendPasswordResetEmail(eq("test@example.com"), anyString()); // Xác minh email đã được gửi
    }

    @Test
    void resetPassword_ShouldResetPassword_WhenTokenIsValid() {
        // Given
        String token = "valid-token";
        String newPass = "newPassword456";
        String encodedNewPass = "encodedNewPassword";
        PasswordResetToken resetToken = new PasswordResetToken(token, user, LocalDateTime.now().plusHours(1));

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode(newPass)).thenReturn(encodedNewPass);

        // When
        userService.resetPassword(token, newPass);

        // Then
        assertThat(user.getPassword()).isEqualTo(encodedNewPass);
        verify(userRepository).save(user);
        verify(tokenRepository).delete(resetToken);
    }

    @Test
    void resetPassword_ShouldThrowException_WhenTokenIsExpired() {
        // Given
        String token = "expired-token";
        PasswordResetToken resetToken = new PasswordResetToken(token, user, LocalDateTime.now().minusHours(1)); // Đã hết hạn

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(resetToken));

        // When & Then
        assertThatThrownBy(() -> userService.resetPassword(token, "newPass"))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Invalid or expired password reset token");

        verify(tokenRepository).delete(resetToken); // Vẫn xóa token dù hết hạn
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_ShouldThrowException_WhenTokenIsInvalid() {
        // Given
        String token = "invalid-token";
        when(tokenRepository.findByToken(token)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.resetPassword(token, "newPass"))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Invalid or expired password reset token");

        verify(userRepository, never()).save(any(User.class));
    }
}