package com.nhom13.ecommerce.config;

import com.nhom13.ecommerce.entity.*;
import com.nhom13.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
@Transactional // <- Thêm transactional để tránh LazyInitializationException
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final PromotionBannerRepository promotionBannerRepository;
    private final PasswordEncoder passwordEncoder;
    private final WishlistItemRepository wishlistItemRepository;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            log.info("Data already exists. Skipping 'dev' data seeding.");
            return;
        }
        log.info("Starting data seeding for 'dev' profile...");
        try {
            List<Category> categories = seedCategories();
            List<User> users = seedUsers();
            List<Product> products = seedProductsAndVariants(categories);

            User customer = users.stream().filter(u -> u.getRole() == Role.CUSTOMER).findFirst().orElseThrow();
            List<Address> addresses = seedAddresses(customer);
            seedBanners();
            seedOrdersAndReviews(customer, products, addresses);

            log.info("Data seeding completed successfully!");
        } catch (Exception e) {
            log.error("Error during data seeding: {}", e.getMessage(), e);
        }
    }

    private List<Product> seedProductsAndVariants(List<Category> categories) {
        log.info("Seeding products and variants...");

        Category catKeyboard = categories.stream().filter(c -> c.getName().equals("Bàn phím")).findFirst().get();
        Category catMouse = categories.stream().filter(c -> c.getName().equals("Chuột")).findFirst().get();

        Product p_keyboard = createProduct("Bàn phím cơ AKKO 5075S", "Layout 75%, Gasket mount.",
                new BigDecimal("2500000"), catKeyboard, true,
                Map.of("Layout", "75% (TKL)", "Kết nối", "3 Mode (USB-C, 2.4Ghz, Bluetooth)"));
        productRepository.save(p_keyboard);

        ProductVariant kv1 = createVariant(p_keyboard, "Trắng", "N/A", "AKKO-5075-WHT", 20, null);
        ProductVariant kv2 = createVariant(p_keyboard, "Đen", "N/A", "AKKO-5075-BLK", 30, null);
        productVariantRepository.saveAll(Arrays.asList(kv1, kv2));

        Product p_mouse = createProduct("Chuột Logitech MX Master 3S", "Chuột văn phòng quốc dân.",
                new BigDecimal("2300000"), catMouse, false,
                Map.of("DPI", "8000 DPI", "Nút bấm", "Silent Click"));
        productRepository.save(p_mouse);

        ProductVariant mv1 = createVariant(p_mouse, "Xám Đen", "N/A", "LOGI-MX3S-GRY", 25, null);
        ProductVariant mv2 = createVariant(p_mouse, "Trắng Xám", "N/A", "LOGI-MX3S-WHT", 15, null);
        productVariantRepository.saveAll(Arrays.asList(mv1, mv2));

        return productRepository.findAll();
    }

    private void seedOrdersAndReviews(User customer, List<Product> products, List<Address> addresses) {
        log.info("Seeding orders and reviews...");
        Address defaultAddress = addresses.stream().filter(Address::getIsDefault).findFirst().get();
        String addressSnapshot = defaultAddress.getFullAddress();

        ProductVariant keyboard_white = productVariantRepository.findAll().stream()
                .filter(v -> v.getSku().equals("AKKO-5075-WHT")).findFirst().get();
        ProductVariant mouse_grey = productVariantRepository.findAll().stream()
                .filter(v -> v.getSku().equals("LOGI-MX3S-GRY")).findFirst().get();

        Order o1 = new Order();
        o1.setUser(customer);
        o1.setShippingAddress(defaultAddress);
        o1.setShippingAddressSnapshot(addressSnapshot);
        o1.setStatus(OrderStatus.DELIVERED);
        o1.setPaymentMethod("COD");
        o1.setPaymentStatus("PAID");
        o1.setCreatedAt(LocalDateTime.now().minusDays(10));

        OrderItem oi1_1 = new OrderItem();
        oi1_1.setOrder(o1);
        oi1_1.setProductVariant(keyboard_white);
        oi1_1.setQuantity(1);
        oi1_1.setUnitPrice(keyboard_white.getProduct().getPrice());
        oi1_1.setTotalPrice(keyboard_white.getProduct().getPrice());

        OrderItem oi1_2 = new OrderItem();
        oi1_2.setOrder(o1);
        oi1_2.setProductVariant(mouse_grey);
        oi1_2.setQuantity(1);
        oi1_2.setUnitPrice(mouse_grey.getProduct().getPrice());
        oi1_2.setTotalPrice(mouse_grey.getProduct().getPrice());

        o1.setOrderItems(Arrays.asList(oi1_1, oi1_2));
        o1.setTotalAmount(oi1_1.getTotalPrice().add(oi1_2.getTotalPrice()));

        orderRepository.save(o1);

        log.info("Seeding reviews for delivered order...");
        Review r1 = new Review();
        r1.setUser(customer);
        r1.setProduct(keyboard_white.getProduct());
        r1.setRating(5);
        r1.setComment("Bàn phím gõ êm, màu trắng rất đẹp!");

        reviewRepository.save(r1);

        Product p_keyboard = keyboard_white.getProduct();
        p_keyboard.setAverageRating(5.0);
        p_keyboard.setReviewCount(1);
        productRepository.save(p_keyboard);
    }

    private Product createProduct(String name, String desc, BigDecimal price, Category cat, boolean featured, Map<String, String> specs) {
        Product p = new Product();
        p.setName(name);
        p.setDescription(desc);
        p.setPrice(price);
        p.setCategory(cat);
        p.setIsActive(true);
        p.setIsFeatured(featured);
        p.setSpecifications(specs);
        p.setAverageRating(0.0);
        p.setReviewCount(0);
        p.setViewCount(0L);
        p.setImageUrl("https://placehold.co/600x600/555555/FFFFFF?text=" + name.replace(" ", "+"));
        return p;
    }

    private ProductVariant createVariant(Product product, String color, String size, String sku, int stock, BigDecimal price) {
        ProductVariant v = new ProductVariant();
        v.setProduct(product);
        v.setColor(color);
        v.setProductSize(size);
        v.setSku(sku);
        v.setStockQuantity(stock);
        v.setPrice(price); // null => sử dụng giá sản phẩm cha
        return v;
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

    private List<Address> seedAddresses(User customer) {
        log.info("Seeding addresses for customer...");
        Address a1 = new Address();
        a1.setUser(customer);
        a1.setFullName(customer.getFullName());
        a1.setPhone(customer.getPhone());
        a1.setStreet("123 Đường ABC");
        a1.setWard("Phường 1");
        a1.setDistrict("Quận 1");
        a1.setCity("TP. Hồ Chí Minh");
        a1.setIsDefault(true);
        return addressRepository.saveAll(List.of(a1));
    }

    private void seedBanners() {
        log.info("Seeding promotion banners...");
        PromotionBanner b1 = new PromotionBanner();
        b1.setTitle("Siêu Sale Bàn phím");
        b1.setImageUrl("https://placehold.co/1200x400/003366/FFFFFF?text=SALE+BAN+PHIM");
        b1.setTargetUrl("/products/category/4"); // ID 4 là Bàn phím
        b1.setIsActive(true);
        promotionBannerRepository.save(b1);
    }
}
