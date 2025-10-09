package com.nhom13.ecommerce.service;

import com.nhom13.ecommerce.dto.ProductDTO;
import com.nhom13.ecommerce.dto.SearchResultDTO;
import com.nhom13.ecommerce.entity.Product;
import com.nhom13.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {
    
    private final ProductRepository productRepository;
    
    public SearchResultDTO searchProducts(
            String query,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String sortBy,
            String sortDirection,
            int page,
            int size) {
        
        Sort sort = createSort(sortBy, sortDirection);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Product> productPage = productRepository.findProductsWithFilters(
            query, categoryId, minPrice, maxPrice, pageable);
        
        SearchResultDTO result = new SearchResultDTO();
        result.setProducts(productPage.getContent().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList()));
        result.setTotalElements(productPage.getTotalElements());
        result.setTotalPages(productPage.getTotalPages());
        result.setCurrentPage(page);
        result.setSize(size);
        
        return result;
    }
    
    public List<ProductDTO> getRecommendedProducts(Long productId, int limit) {
        // Simple recommendation based on same category
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return List.of();
        }
        
        Pageable pageable = PageRequest.of(0, limit);
        return productRepository.findByCategoryIdAndIsActiveTrue(product.getCategory().getId())
            .stream()
            .filter(p -> !p.getId().equals(productId))
            .limit(limit)
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    private Sort createSort(String sortBy, String sortDirection) {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") 
            ? Sort.Direction.DESC 
            : Sort.Direction.ASC;
        
        return Sort.by(direction, sortBy);
    }
    
    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setSku(product.getSku());
        dto.setCategoryId(product.getCategory().getId());
        dto.setCategoryName(product.getCategory().getName());
        dto.setImageUrl(product.getImageUrl());
        dto.setIsActive(product.getIsActive());
        return dto;
    }
}