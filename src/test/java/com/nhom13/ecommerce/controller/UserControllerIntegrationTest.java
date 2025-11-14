package com.nhom13.ecommerce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhom13.ecommerce.config.TestConfig;
import com.nhom13.ecommerce.dto.LoginRequest;
import com.nhom13.ecommerce.dto.RegisterRequest;
import static org.assertj.core.api.Assertions.assertThat;
// === PHẦN BỔ SUNG MỚI ===
import com.nhom13.ecommerce.dto.ChangePasswordRequest;
import com.nhom13.ecommerce.dto.UserProfileUpdateDTO;
// === KẾT THÚC PHẦN BỔ SUNG ===
import com.nhom13.ecommerce.entity.Role;
import com.nhom13.ecommerce.entity.User;
import com.nhom13.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
// === PHẦN BỔ SUNG MỚI ===
import org.springframework.security.test.context.support.WithMockUser;
// === KẾT THÚC PHẦN BỔ SUNG ===
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// === PHẦN BỔ SUNG MỚI ===
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
// === KẾT THÚC PHẦN BỔ SUNG ===
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestConfig.class)
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    // === PHẦN BỔ SUNG MỚI ===
    private User customerUser;
    private User adminUser;
    // === KẾT THÚC PHẦN BỔ SUNG ===

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        
        // === PHẦN BỔ SUNG MỚI: Tạo sẵn user để test @WithMockUser ===
        customerUser = new User();
        customerUser.setEmail("customer@example.com");
        customerUser.setPassword(passwordEncoder.encode("customer123"));
        customerUser.setFirstName("Customer");
        customerUser.setLastName("User");
        customerUser.setRole(Role.CUSTOMER);
        customerUser.setIsActive(true);
        userRepository.save(customerUser);

        adminUser = new User();
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword(passwordEncoder.encode("admin123"));
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setRole(Role.ADMIN);
        adminUser.setIsActive(true);
        userRepository.save(adminUser);
        // === KẾT THÚC PHẦN BỔ SUNG ===
    }

    @Test
    void register_ShouldCreateUser_WhenValidRequest() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setPhone("1234567890");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()) // Mong đợi 201
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    void login_ShouldReturnToken_WhenValidCredentials() throws Exception {
        // Given
        // User đã được tạo trong setUp (customerUser)
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("customer@example.com");
        loginRequest.setPassword("customer123");
        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk()) // Mong đợi 200
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("customer@example.com"));
    }

    // === PHẦN BỔ SUNG MỚI ===

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void getCurrentUser_ShouldReturnUserDetails_WhenAuthenticated() throws Exception {
        // Given (User "customer@example.com" đã đăng nhập)

        // When & Then
        mockMvc.perform(get("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("customer@example.com"))
                .andExpect(jsonPath("$.firstName").value("Customer"));
    }

    @Test
    void getCurrentUser_ShouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {
        // Given (Không có @WithMockUser)

        // When & Then
        mockMvc.perform(get("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized()); // Mong đợi 401
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void updateCurrentUser_ShouldUpdateProfile_WhenAuthenticated() throws Exception {
        // Given
        UserProfileUpdateDTO dto = new UserProfileUpdateDTO();
        dto.setFirstName("UpdatedFirstName");
        dto.setLastName("UpdatedLastName");
        dto.setPhone("111222333");

        // When & Then
        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("UpdatedFirstName"))
                .andExpect(jsonPath("$.lastName").value("UpdatedLastName"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void changePassword_ShouldSucceed_WhenOldPasswordIsCorrect() throws Exception {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("customer123");
        request.setNewPassword("newValidPassword456");

        // When & Then
        mockMvc.perform(post("/api/users/me/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully."));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void changePassword_ShouldFail_WhenOldPasswordIsIncorrect() throws Exception {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("WRONG_PASSWORD");
        request.setNewPassword("newValidPassword456");

        // When & Then
        mockMvc.perform(post("/api/users/me/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()) // Mong đợi 400
                .andExpect(jsonPath("$.message").value("Incorrect old password"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void getUserById_ShouldReturnUser_WhenAdmin() throws Exception {
        // Given (Admin đã đăng nhập)
        // customerUser đã được tạo trong setUp

        // When & Then
        mockMvc.perform(get("/api/users/" + customerUser.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("customer@example.com"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void getUserById_ShouldReturnForbidden_WhenCustomer() throws Exception {
        // Given (Customer đã đăng nhập)
        // adminUser đã được tạo trong setUp

        // When & Then
        mockMvc.perform(get("/api/users/" + adminUser.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden()); // Mong đợi 403
    }
    
    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void deleteUser_ShouldDeactivateUser_WhenAdmin() throws Exception {
        // Given (Admin đã đăng nhập)
        // customerUser đã được tạo trong setUp

        // When & Then
        mockMvc.perform(delete("/api/users/" + customerUser.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent()); // Mong đợi 204
        
        // Xác minh user đã bị vô hiệu hóa
        User deletedUser = userRepository.findById(customerUser.getId()).get();
        assertThat(deletedUser.getIsActive()).isFalse();
    }
}