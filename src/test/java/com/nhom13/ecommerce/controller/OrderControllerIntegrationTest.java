package com.nhom13.ecommerce.controller;













import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhom13.ecommerce.config.TestConfig;
import com.nhom13.ecommerce.entity.*;
import com.nhom13.ecommerce.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestConfig.class)
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    // Inject các Repository để set up data và verify
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductVariantRepository productVariantRepository;
    @Autowired
    private AddressRepository addressRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private OrderRepository orderRepository;

    private User customerUser;
    private Address userAddress;
    private ProductVariant productVariant;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        // Xóa sạch CSDL
        cartItemRepository.deleteAll();
        orderRepository.deleteAll();
        addressRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Tạo User
        customerUser = new User();
        customerUser.setEmail("customer.order@example.com");
        customerUser.setPassword(passwordEncoder.encode("password123"));
        customerUser.setRole(Role.CUSTOMER);
        customerUser.setFirstName("Order");
        customerUser.setLastName("Test");
        customerUser.setIsActive(true);
        userRepository.save(customerUser);

        // 2. Tạo Address
        userAddress = new Address();
        userAddress.setUser(customerUser);
        userAddress.setFullName("Order Test");
        userAddress.setPhone("123456");
        userAddress.setStreet("123 Test St");
        userAddress.setWard("P1");
        userAddress.setDistrict("Q1");
        userAddress.setCity("HCM");
        userAddress.setIsDefault(true);
        addressRepository.save(userAddress);

        // 3. Tạo Category
        Category category = new Category();
        category.setName("Test Category");
        categoryRepository.save(category);

        // 4. Tạo Product
        Product product = new Product();
        product.setName("Test Order Product");
        product.setCategory(category);
        product.setPrice(new BigDecimal("100.00"));
        product.setIsActive(true);
        productRepository.save(product);

        // 5. Tạo ProductVariant (Kho)
        productVariant = new ProductVariant();
        productVariant.setProduct(product);
        productVariant.setSku("ORDER-TEST-SKU");
        productVariant.setStockQuantity(10); // Tồn kho 10
        productVariantRepository.save(productVariant);

        // 6. Tạo CartItem (Giỏ hàng)
        cartItem = new CartItem();
        cartItem.setUser(customerUser);
        cartItem.setProductVariant(productVariant);
        cartItem.setQuantity(2); // Mua 2
        cartItemRepository.save(cartItem);
    }

    @Test
    @WithMockUser(username = "customer.order@example.com", roles = "CUSTOMER")
    void createOrder_ShouldSucceed_AndClearCart_AndDeductStock() throws Exception {
        // Given (User đã đăng nhập, có 1 item trong giỏ hàng)
        long initialStock = productVariant.getStockQuantity(); // 10
        long initialCartCount = cartItemRepository.count(); // 1

        // When
        mockMvc.perform(post("/api/orders")
                        .param("addressId", userAddress.getId().toString())
                        .param("paymentMethod", "COD")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated()) // 201 Created
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.orderItems[0].quantity").value(2));

        // Then (Verify CSDL)
        // 1. Giỏ hàng đã bị xóa 
        long finalCartCount = cartItemRepository.count();
        assertThat(finalCartCount).isEqualTo(0);
        assertThat(cartItemRepository.findByUserIdOrderByCreatedAtDesc(customerUser.getId())).isEmpty();


        // 2. Kho đã bị trừ 
        ProductVariant updatedVariant = productVariantRepository.findById(productVariant.getId()).get();
        assertThat(updatedVariant.getStockQuantity()).isEqualTo(initialStock - cartItem.getQuantity()); // 10 - 2 = 8

        // 3. Đơn hàng đã được tạo
        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "customer.order@example.com", roles = "CUSTOMER")
    void createOrder_ShouldFail_WhenCartIsEmpty() throws Exception {
        // Given
        cartItemRepository.deleteAll(); // Xóa sạch giỏ hàng
        assertThat(cartItemRepository.count()).isEqualTo(0);

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .param("addressId", userAddress.getId().toString())
                        .param("paymentMethod", "COD")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()) // 400 Bad Request
                .andExpect(jsonPath("$.message").value("Cart is empty"));

        // Verify CSDL
        assertThat(orderRepository.count()).isEqualTo(0); // Không có đơn hàng nào được tạo
    }

    @Test
    @WithMockUser(username = "customer.order@example.com", roles = "CUSTOMER")
    void createOrder_ShouldFail_WhenInsufficientStock() throws Exception {
        // Given
        // Cập nhật giỏ hàng: muốn mua 20 (chỉ có 10 trong kho)
        cartItem.setQuantity(20);
        cartItemRepository.save(cartItem);

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .param("addressId", userAddress.getId().toString())
                        .param("paymentMethod", "COD")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()) // 400 Bad Request
                .andExpect(jsonPath("$.message").value("Insufficient stock for product: Test Order Product"));

        // Verify CSDL
        // Đơn hàng không được tạo
        assertThat(orderRepository.count()).isEqualTo(0);
        // Giỏ hàng KHÔNG bị xóa
        assertThat(cartItemRepository.count()).isEqualTo(1);
        // Kho KHÔNG bị trừ
        ProductVariant variant = productVariantRepository.findById(productVariant.getId()).get();
        assertThat(variant.getStockQuantity()).isEqualTo(10);
    }

    @Test
    @WithMockUser(username = "customer.order@example.com", roles = "ADMIN")
    void getAllOrders_ShouldSucceed_WhenRoleIsAdmin() throws Exception {
        // Given (User là Admin)
        // (Không cần tạo đơn hàng vì test này chỉ kiểm tra quyền)

        // When & Then
        mockMvc.perform(get("/api/orders/all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()); // 200 OK
    }

    @Test
    @WithMockUser(username = "customer.order@example.com", roles = "CUSTOMER")
    void getAllOrders_ShouldFail_WhenRoleIsCustomer() throws Exception {
        // Given (User là Customer)

        // When & Then
        mockMvc.perform(get("/api/orders/all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden()); // 403 Forbidden
    }
}