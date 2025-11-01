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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
// Chỉ chạy seeder này khi profile 'dev' được kích hoạt
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final PromotionBannerRepository promotionBannerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Nếu đã có user, không chạy seeder
        if (userRepository.count() > 0) {
            log.info("Data already exists. Skipping 'dev' data seeding.");
            return;
        }
        
        log.info("Starting data seeding for 'dev' profile...");

        try {
            // Thứ tự seeder rất quan trọng
            List<Category> categories = seedCategories();
            List<User> users = seedUsers();
            List<Product> products = seedProducts(categories);
            
            // Lấy ra customer để gán dữ liệu
            User customer = users.stream()
                .filter(u -> u.getRole() == Role.CUSTOMER)
                .findFirst()
                .orElseThrow();

            List<Address> addresses = seedAddresses(customer);
            seedWishlist(customer, products);
            seedBanners();
            seedOrdersAndReviews(customer, products, addresses);
            
            log.info("Data seeding completed successfully!");

        } catch (Exception e) {
            log.error("Error during data seeding: {}", e.getMessage(), e);
        }
    }
    
    private List<User> seedUsers() {
        log.info("Seeding users...");
        
        User admin = new User();
        admin.setEmail("admin@ecommerce.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setFirstName("Admin");
        admin.setLastName("Root");
        admin.setPhone("0123456789");
        admin.setRole(Role.ADMIN);
        admin.setIsActive(true);
        
        User customer = new User();
        customer.setEmail("customer@ecommerce.com");
        customer.setPassword(passwordEncoder.encode("customer123"));
        customer.setFirstName("Hoàng");
        customer.setLastName("Lâm");
        customer.setPhone("0987654321");
        customer.setRole(Role.CUSTOMER);
        customer.setIsActive(true);
        
        return userRepository.saveAll(Arrays.asList(admin, customer));
    }
    
    private List<Category> seedCategories() {
        log.info("Seeding categories...");
        
        Category c1 = new Category(); c1.setName("Laptop"); c1.setDescription("Laptop, Macbook các loại"); c1.setIsActive(true);
        Category c2 = new Category(); c2.setName("PC - Máy tính bộ"); c2.setDescription("Máy tính để bàn, All-in-One"); c2.setIsActive(true);
        Category c3 = new Category(); c3.setName("Màn hình"); c3.setDescription("Màn hình máy tính 2K, 4K, Gaming"); c3.setIsActive(true);
        Category c4 = new Category(); c4.setName("Bàn phím"); c4.setDescription("Bàn phím cơ, giả cơ, văn phòng"); c4.setIsActive(true);
        Category c5 = new Category(); c5.setName("Chuột"); c5.setDescription("Chuột gaming, chuột văn phòng"); c5.setIsActive(true);
        
        return categoryRepository.saveAll(Arrays.asList(c1, c2, c3, c4, c5));
    }

    private List<Product> seedProducts(List<Category> categories) {
        log.info("Seeding products...");
        
        Category catLaptop = categories.stream().filter(c -> c.getName().equals("Laptop")).findFirst().get();
        Category catPC = categories.stream().filter(c -> c.getName().equals("PC - Máy tính bộ")).findFirst().get();
        Category catMonitor = categories.stream().filter(c -> c.getName().equals("Màn hình")).findFirst().get();
        Category catKeyboard = categories.stream().filter(c -> c.getName().equals("Bàn phím")).findFirst().get();
        Category catMouse = categories.stream().filter(c -> c.getName().equals("Chuột")).findFirst().get();

        List<Product> products = Arrays.asList(
            createProduct("Macbook Pro 16 M3 Max", "Siêu phẩm đồ họa, màn hình Liquid Retina XDR 16.2 inch.", new BigDecimal("65000000"), 10, catLaptop, "SKU001", true, 
                Map.of("CPU", "Apple M3 Max 16-core", "RAM", "32GB", "SSD", "1TB", "Màn hình", "16.2-inch Liquid Retina XDR")),
            
            createProduct("Dell XPS 15 9530", "Văn phòng cao cấp, thiết kế nhôm nguyên khối, viền mỏng InfinityEdge.", new BigDecimal("45000000"), 15, catLaptop, "SKU002", false, 
                Map.of("CPU", "Core Ultra 7 155H", "RAM", "32GB DDR5", "SSD", "1TB NVMe", "VGA", "NVIDIA RTX 4060 8GB")),
            
            createProduct("PC Gaming HyperX", "Chiến mọi loại game với RTX 4070, tản nhiệt nước AIO.", new BigDecimal("30000000"), 5, catPC, "SKU003", true, 
                Map.of("CPU", "Intel Core i7-14700K", "RAM", "32GB Kingston Fury Beast", "VGA", "NVIDIA GeForce RTX 4070 12GB", "Mainboard", "ASUS Z790")),
            
            createProduct("Dell Alienware 32 4K QD-OLED", "Màn hình 240Hz 4K đầu tiên trên thế giới.", new BigDecimal("28000000"), 8, catMonitor, "SKU004", true, 
                Map.of("Kích thước", "32 inch", "Độ phân giải", "4K (3840x2160)", "Tần số quét", "240Hz", "Tấm nền", "QD-OLED")),
            
            createProduct("Bàn phím cơ AKKO 5075S", "Layout 75%, Gasket mount, gõ siêu êm.", new BigDecimal("2500000"), 50, catKeyboard, "SKU005", false, 
                Map.of("Switch", "AKKO Cream Yellow V3 Pro", "Layout", "75% (TKL)", "Kết nối", "3 Mode (USB-C, 2.4Ghz, Bluetooth)")),
            
            createProduct("Chuột Logitech MX Master 3S", "Chuột văn phòng quốc dân, cuộn Magspeed siêu nhanh, 8000 DPI.", new BigDecimal("2300000"), 30, catMouse, "SKU006", false, 
                Map.of("Kết nối", "Bluetooth/Bolt Receiver", "DPI", "8000 DPI", "Nút bấm", "Silent Click"))
        );
        return productRepository.saveAll(products);
    }

    private List<Address> seedAddresses(User customer) {
        log.info("Seeding addresses for customer...");
        
        Address a1 = new Address();
        a1.setUser(customer);
        a1.setFullName(customer.getFirstName() + " " + customer.getLastName());
        a1.setPhone(customer.getPhone());
        a1.setStreet("123 Đường ABC");
        a1.setWard("Phường 1");
        a1.setDistrict("Quận 1");
        a1.setCity("TP. Hồ Chí Minh");
        a1.setIsDefault(true);

        Address a2 = new Address();
        a2.setUser(customer);
        a2.setFullName(customer.getFirstName() + " " + customer.getLastName());
        a2.setPhone(customer.getPhone());
        a2.setStreet("456 Đường XYZ");
        a2.setWard("Phường Bến Nghé");
        a2.setDistrict("Quận Thủ Đức");
        a2.setCity("TP. Hồ Chí Minh");
        a2.setIsDefault(false);
        
        return addressRepository.saveAll(Arrays.asList(a1, a2));
    }

    private void seedWishlist(User customer, List<Product> products) {
        log.info("Seeding wishlist for customer...");
        // Add product 2 (Dell XPS) and 4 (Monitor) to wishlist
        WishlistItem w1 = new WishlistItem(customer, products.get(1));
        WishlistItem w2 = new WishlistItem(customer, products.get(3));
        
        wishlistItemRepository.saveAll(Arrays.asList(w1, w2));
    }

    private void seedBanners() {
        log.info("Seeding promotion banners...");
        
        PromotionBanner b1 = new PromotionBanner();
        b1.setTitle("Siêu Sale Laptop");
        b1.setImageUrl("https://placehold.co/1200x400/003366/FFFFFF?text=SIEU+SALE+LAPTOP");
        b1.setTargetUrl("/api/search?categoryId=1"); // ID 1 là Laptop
        b1.setIsActive(true);

        PromotionBanner b2 = new PromotionBanner();
        b2.setTitle("Build PC Cực Đã");
        b2.setImageUrl("https://placehold.co/1200x400/2E8B57/FFFFFF?text=BUILD+PC+CUC+DA");
        b2.setTargetUrl("/api/search?categoryId=2"); // ID 2 là PC
        b2.setIsActive(true);
        
        promotionBannerRepository.saveAll(Arrays.asList(b1, b2));
    }

    private void seedOrdersAndReviews(User customer, List<Product> products, List<Address> addresses) {
        log.info("Seeding orders and reviews...");
        Address defaultAddress = addresses.stream().filter(Address::getIsDefault).findFirst().get();
        String addressSnapshot = defaultAddress.getFullAddress();

        // === Order 1: DELIVERED (để seed reviews) ===
        Order o1 = new Order();
        o1.setUser(customer);
        o1.setShippingAddress(defaultAddress);
        o1.setShippingAddressSnapshot(addressSnapshot);
        o1.setStatus(OrderStatus.DELIVERED);
        o1.setPaymentMethod("COD");
        o1.setPaymentStatus("PAID");
        o1.setTrackingNumber("GHTK123456");
        o1.setCreatedAt(LocalDateTime.now().minusDays(10)); // Đơn hàng cũ
        
        Product p_keyboard = products.get(4); // AKKO
        Product p_mouse = products.get(5); // Logitech
        
        OrderItem oi1_1 = new OrderItem();
        oi1_1.setOrder(o1);
        oi1_1.setProduct(p_keyboard);
        oi1_1.setQuantity(1);
        oi1_1.setUnitPrice(p_keyboard.getPrice());
        oi1_1.setTotalPrice(p_keyboard.getPrice().multiply(new BigDecimal(1)));
        
        OrderItem oi1_2 = new OrderItem();
        oi1_2.setOrder(o1);
        oi1_2.setProduct(p_mouse);
        oi1_2.setQuantity(1);
        oi1_2.setUnitPrice(p_mouse.getPrice());
        oi1_2.setTotalPrice(p_mouse.getPrice().multiply(new BigDecimal(1)));

        o1.setOrderItems(Arrays.asList(oi1_1, oi1_2));
        o1.setTotalAmount(p_keyboard.getPrice().add(p_mouse.getPrice()));
        
        // === Order 2: PENDING (Đơn hàng mới) ===
        Order o2 = new Order();
        o2.setUser(customer);
        o2.setShippingAddress(defaultAddress);
        o2.setShippingAddressSnapshot(addressSnapshot);
        o2.setStatus(OrderStatus.PENDING);
        o2.setPaymentMethod("VNPAY");
        o2.setPaymentStatus("PENDING");
        o2.setCreatedAt(LocalDateTime.now().minusMinutes(30)); // Đơn hàng mới
        
        Product p_macbook = products.get(0); // Macbook
        OrderItem oi2_1 = new OrderItem();
        oi2_1.setOrder(o2);
        oi2_1.setProduct(p_macbook);
        oi2_1.setQuantity(1);
        oi2_1.setUnitPrice(p_macbook.getPrice());
        oi2_1.setTotalPrice(p_macbook.getPrice().multiply(new BigDecimal(1)));
        
        o2.setOrderItems(List.of(oi2_1));
        o2.setTotalAmount(p_macbook.getPrice());
        
        // Lưu cả 2 đơn hàng
        orderRepository.saveAll(Arrays.asList(o1, o2));
        
        // === Seed Reviews (Cho Order 1 đã giao) ===
        log.info("Seeding reviews for delivered order...");
        Review r1 = new Review();
        r1.setUser(customer);
        r1.setProduct(p_keyboard); // Review bàn phím
        r1.setRating(5);
        r1.setComment("Bàn phím gõ rất êm, build chắc chắn. Rất hài lòng!");
        
        Review r2 = new Review();
        r2.setUser(customer);
        r2.setProduct(p_mouse); // Review chuột
        r2.setRating(4);
        r2.setComment("Chuột dùng tốt, pin trâu, nhưng hơi nặng tay một chút.");
        
        reviewRepository.saveAll(Arrays.asList(r1, r2));

        // === Cập nhật lại thông tin rating cho sản phẩm ===
        // (Vì seeder không trigger ReviewService)
        p_keyboard.setAverageRating(5.0);
        p_keyboard.setReviewCount(1);
        
        p_mouse.setAverageRating(4.0);
        p_mouse.setReviewCount(1);
        productRepository.saveAll(Arrays.asList(p_keyboard, p_mouse));
    }

    // Helper tạo sản phẩm
    private Product createProduct(String name, String desc, BigDecimal price, int stock, 
                                  Category cat, String sku, boolean featured, Map<String, String> specs) {
        Product p = new Product();
        p.setName(name);
        p.setDescription(desc);
        p.setPrice(price);
        p.setStockQuantity(stock);
        p.setCategory(cat);
        p.setSku(sku);
        p.setIsActive(true);
        p.setIsFeatured(featured);
        p.setSpecifications(specs);
        p.setAverageRating(0.0);
        p.setReviewCount(0);
        // 
        p.setImageUrl("https://placehold.co/600x600/555555/FFFFFF?text=" + name.replace(" ", "+"));
        return p;
    }
}
