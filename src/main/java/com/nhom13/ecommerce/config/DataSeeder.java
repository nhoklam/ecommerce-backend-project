package com.nhom13.ecommerce.config;

import com.nhom13.ecommerce.entity.*;
import com.nhom13.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

// @Component
@RequiredArgsConstructor
@Slf4j
@Profile("prod") // Không chạy trong production
public class DataSeeder implements CommandLineRunner {
    
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            seedData();
        }
    }
    
    private void seedData() {
        log.info("Starting data seeding...");
        
        // Seed Users
        seedUsers();
        
        // Seed Categories
        seedCategories();
        
        // Seed Products
        seedProducts();
        
        log.info("Data seeding completed!");
    }
    
    private void seedUsers() {
        log.info("Seeding users...");
        
        // Admin user
        User admin = new User();
        admin.setEmail("admin@ecommerce.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setPhone("0123456789");
        admin.setRole(Role.ADMIN);
        admin.setIsActive(true);
        userRepository.save(admin);
        
        // Customer user
        User customer = new User();
        customer.setEmail("customer@ecommerce.com");
        customer.setPassword(passwordEncoder.encode("customer123"));
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setPhone("0987654321");
        customer.setRole(Role.CUSTOMER);
        customer.setIsActive(true);
        userRepository.save(customer);
        
        log.info("Users seeded successfully!");
    }
    
    private void seedCategories() {
        log.info("Seeding categories...");
        
        List<Category> categories = Arrays.asList(
            createCategory("Áo Nam", "Các loại áo dành cho nam giới"),
            createCategory("Áo Nữ", "Các loại áo dành cho nữ giới"),
            createCategory("Quần Nam", "Các loại quần dành cho nam giới"),
            createCategory("Quần Nữ", "Các loại quần dành cho nữ giới"),
            createCategory("Giày Dép", "Giày dép nam nữ"),
            createCategory("Phụ Kiện", "Các phụ kiện thời trang")
        );
        
        categoryRepository.saveAll(categories);
        log.info("Categories seeded successfully!");
    }
    
    private void seedProducts() {
        log.info("Seeding products...");
        
        List<Category> categories = categoryRepository.findAll();
        
        // Áo Nam
        Category aoNam = categories.stream()
            .filter(c -> c.getName().equals("Áo Nam"))
            .findFirst().orElse(null);
        
        if (aoNam != null) {
            List<Product> aoNamProducts = Arrays.asList(
                createProduct("Áo Thun Nam Basic", "Áo thun cotton cao cấp", new BigDecimal("199000"), 50, aoNam),
                createProduct("Áo Polo Nam", "Áo polo thể thao", new BigDecimal("299000"), 30, aoNam),
                createProduct("Áo Sơ Mi Nam", "Áo sơ mi công sở", new BigDecimal("399000"), 25, aoNam)
            );
            productRepository.saveAll(aoNamProducts);
        }
        
        // Áo Nữ
        Category aoNu = categories.stream()
            .filter(c -> c.getName().equals("Áo Nữ"))
            .findFirst().orElse(null);
        
        if (aoNu != null) {
            List<Product> aoNuProducts = Arrays.asList(
                createProduct("Áo Thun Nữ", "Áo thun nữ thời trang", new BigDecimal("179000"), 40, aoNu),
                createProduct("Áo Kiểu Nữ", "Áo kiểu công sở", new BigDecimal("349000"), 20, aoNu),
                createProduct("Áo Croptop", "Áo croptop trẻ trung", new BigDecimal("149000"), 35, aoNu)
            );
            productRepository.saveAll(aoNuProducts);
        }
        
        // Quần Nam
        Category quanNam = categories.stream()
            .filter(c -> c.getName().equals("Quần Nam"))
            .findFirst().orElse(null);
        
        if (quanNam != null) {
            List<Product> quanNamProducts = Arrays.asList(
                createProduct("Quần Jean Nam", "Quần jean cao cấp", new BigDecimal("449000"), 30, quanNam),
                createProduct("Quần Kaki Nam", "Quần kaki công sở", new BigDecimal("359000"), 25, quanNam),
                createProduct("Quần Short Nam", "Quần short thể thao", new BigDecimal("199000"), 40, quanNam)
            );
            productRepository.saveAll(quanNamProducts);
        }
        
        // Quần Nữ
        Category quanNu = categories.stream()
            .filter(c -> c.getName().equals("Quần Nữ"))
            .findFirst().orElse(null);
        
        if (quanNu != null) {
            List<Product> quanNuProducts = Arrays.asList(
                createProduct("Quần Jean Nữ", "Quần jean skinny", new BigDecimal("399000"), 35, quanNu),
                createProduct("Chân Váy", "Chân váy A thời trang", new BigDecimal("249000"), 30, quanNu),
                createProduct("Quần Legging", "Quần legging co giãn", new BigDecimal("159000"), 50, quanNu)
            );
            productRepository.saveAll(quanNuProducts);
        }
        
        log.info("Products seeded successfully!");
    }
    
    private Category createCategory(String name, String description) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        category.setIsActive(true);
        return category;
    }
    
    private Product createProduct(String name, String description, BigDecimal price, int stock, Category category) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setStockQuantity(stock);
        product.setSku("SKU-" + name.replaceAll(" ", "").toUpperCase());
        product.setCategory(category);
        product.setIsActive(true);
        product.setImageUrl("https://via.placeholder.com/300x300?text=" + name.replaceAll(" ", "+"));
        return product;
    }
}