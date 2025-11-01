package com.nhom13.ecommerce.service;

import com.nhom13.ecommerce.dto.ProductDTO;
import com.nhom13.ecommerce.entity.Category;
import com.nhom13.ecommerce.entity.Product;
import com.nhom13.ecommerce.exception.BadRequestException;
import com.nhom13.ecommerce.exception.ResourceNotFoundException;
import com.nhom13.ecommerce.repository.CategoryRepository;
import com.nhom13.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    
    public ProductDTO createProduct(ProductDTO productDTO) {
        // Validate category exists
        Category category = categoryRepository.findById(productDTO.getCategoryId())
          .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + productDTO.getCategoryId()));
        
        // Generate SKU if not provided
        //
        if (productDTO.getSku() == null || productDTO.getSku().isEmpty()) {
            productDTO.setSku(generateSku());
        } else if (productRepository.existsBySku(productDTO.getSku())) {
            throw new BadRequestException("SKU already exists: " + productDTO.getSku());
        }
        
        Product product = new Product();
        mapDtoToEntity(productDTO, product);
        product.setCategory(category);
        product.setIsActive(true);
        // [MỚI] Khởi tạo giá trị mặc định
        product.setIsFeatured(productDTO.getIsFeatured()!= null? productDTO.getIsFeatured() : false);
        product.setAverageRating(0.0);
        product.setReviewCount(0);
        
        Product savedProduct = productRepository.save(product);
        return convertToDTO(savedProduct);
    }
    
    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
          .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return convertToDTO(product);
    }
    
    @Transactional(readOnly = true)
    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        return productRepository.findAllActiveProducts(pageable)
          .map(this::convertToDTO);
    }
    
    @Transactional(readOnly = true)
    public Page<ProductDTO> searchProducts(String name, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return productRepository.findProductsWithFilters(name, categoryId, minPrice, maxPrice, pageable)
          .map(this::convertToDTO);
    }
    
    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryIdAndIsActiveTrue(categoryId)
          .stream()
          .map(this::convertToDTO)
          .collect(Collectors.toList());
    }

    // [MỚI] Lấy sản phẩm nổi bật
    @Transactional(readOnly = true)
    public List<ProductDTO> getFeaturedProducts() {
        return productRepository.findByIsFeaturedTrueAndIsActiveTrue()
         .stream()
         .map(this::convertToDTO)
         .collect(Collectors.toList());
    }
    
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        Product product = productRepository.findById(id)
          .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        
        // Check if category exists
        if (!product.getCategory().getId().equals(productDTO.getCategoryId())) {
            Category newCategory = categoryRepository.findById(productDTO.getCategoryId())
              .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + productDTO.getCategoryId()));
            product.setCategory(newCategory);
        }
        
        mapDtoToEntity(productDTO, product);
        Product updatedProduct = productRepository.save(product);
        return convertToDTO(updatedProduct);
    }
    
    // Khôi phục phương thức
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
          .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setIsActive(false);
        productRepository.save(product);
    }
    
    // Khôi phục phương thức
    public void updateStock(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
          .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        int newStock = product.getStockQuantity() - quantity;
        if (newStock < 0) {
            throw new BadRequestException("Insufficient stock for product: " + product.getName());
        }
        
        product.setStockQuantity(newStock);
        productRepository.save(product);
    }
    
    // Khôi phục phương thức
    private void mapDtoToEntity(ProductDTO dto, Product product) {
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStockQuantity(dto.getStockQuantity());
        product.setSku(dto.getSku());
        product.setImageUrl(dto.getImageUrl());
        
        // [MỚI]
        product.setSpecifications(dto.getSpecifications());
        
        // [MỚI] Thêm từ mục 2.4.2 (Featured)
        if(dto.getIsFeatured()!= null) {
            product.setIsFeatured(dto.getIsFeatured());
        }
    }
    
    // Chuyển thành public và khôi phục
    public ProductDTO convertToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setSku(product.getSku());
        
        if (product.getCategory()!= null) {
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategoryName(product.getCategory().getName());
        }
        
        dto.setImageUrl(product.getImageUrl());
        dto.setIsActive(product.getIsActive());

        // [MỚI]
        dto.setAverageRating(product.getAverageRating());
        dto.setReviewCount(product.getReviewCount());
        dto.setSpecifications(product.getSpecifications());
        dto.setIsFeatured(product.getIsFeatured());
        
        return dto;
    }

    // Khôi phục phương thức
    private String generateSku() {
        return "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}