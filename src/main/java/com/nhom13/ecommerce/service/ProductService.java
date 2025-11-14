package com.nhom13.ecommerce.service;

import com.nhom13.ecommerce.dto.ProductDTO;
import com.nhom13.ecommerce.dto.ProductVariantDTO;
import com.nhom13.ecommerce.entity.Category;
import com.nhom13.ecommerce.entity.Product;
import com.nhom13.ecommerce.entity.ProductVariant;
import com.nhom13.ecommerce.exception.BadRequestException;
import com.nhom13.ecommerce.exception.ResourceNotFoundException;
import com.nhom13.ecommerce.repository.CategoryRepository;
import com.nhom13.ecommerce.repository.ProductRepository;
import com.nhom13.ecommerce.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository productVariantRepository;

    @Transactional
    public ProductDTO createProduct(ProductDTO productDTO) {
        Category category = categoryRepository.findById(productDTO.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + productDTO.getCategoryId()));

        Product product = new Product();
        mapDtoToEntity(productDTO, product);
        product.setCategory(category);
        product.setIsActive(true);
        product.setIsFeatured(productDTO.getIsFeatured() != null ? productDTO.getIsFeatured() : false);
        product.setAverageRating(0.0);
        product.setReviewCount(0);
        product.setViewCount(0L);

        Product savedProduct = productRepository.save(product);

        if (productDTO.getVariants() != null && !productDTO.getVariants().isEmpty()) {
            List<ProductVariant> variants = new ArrayList<>();
            for (ProductVariantDTO variantDTO : productDTO.getVariants()) {
                if (variantDTO.getSku() == null || variantDTO.getSku().isBlank()) {
                    throw new BadRequestException("SKU is required for all variants.");
                }
                if (productVariantRepository.existsBySku(variantDTO.getSku())) {
                    throw new BadRequestException("SKU '" + variantDTO.getSku() + "' already exists.");
                }
                ProductVariant variant = new ProductVariant();
                mapVariantDtoToEntity(variantDTO, variant);
                variant.setProduct(savedProduct);
                variants.add(variant);
            }
            productVariantRepository.saveAll(variants);
            savedProduct.setVariants(variants);
        } else {
            throw new BadRequestException("Product must have at least one variant.");
        }

        return convertToDTO(savedProduct);
    }

    @Transactional
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (!product.getCategory().getId().equals(productDTO.getCategoryId())) {
            Category newCategory = categoryRepository.findById(productDTO.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + productDTO.getCategoryId()));
            product.setCategory(newCategory);
        }

        mapDtoToEntity(productDTO, product);

        if (productDTO.getVariants() == null || productDTO.getVariants().isEmpty()) {
            throw new BadRequestException("Product must have at least one variant.");
        }

        List<Long> dtoVariantIds = productDTO.getVariants().stream()
                .map(ProductVariantDTO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        product.getVariants().removeIf(variant -> !dtoVariantIds.contains(variant.getId()));

        for (ProductVariantDTO variantDTO : productDTO.getVariants()) {
            if (variantDTO.getSku() == null || variantDTO.getSku().isBlank()) {
                throw new BadRequestException("SKU is required for all variants.");
            }
            Optional<ProductVariant> existingSkuVariant = productVariantRepository.findBySku(variantDTO.getSku());
            if (existingSkuVariant.isPresent() && !existingSkuVariant.get().getId().equals(variantDTO.getId())) {
                throw new BadRequestException("SKU '" + variantDTO.getSku() + "' is already in use by another variant.");
            }

            if (variantDTO.getId() != null) {
                ProductVariant existingVariant = product.getVariants().stream()
                        .filter(v -> v.getId().equals(variantDTO.getId()))
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("Variant not found to update with id: " + variantDTO.getId()));
                mapVariantDtoToEntity(variantDTO, existingVariant);
            } else {
                ProductVariant newVariant = new ProductVariant();
                mapVariantDtoToEntity(variantDTO, newVariant);
                newVariant.setProduct(product);
                product.getVariants().add(newVariant);
            }
        }

        Product updatedProduct = productRepository.save(product);
        return convertToDTO(updatedProduct);
    }

    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return convertToDTO(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        return productRepository.findAllActiveProducts(pageable).map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> searchProducts(String name, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return productRepository.findProductsWithFilters(name, categoryId, minPrice, maxPrice, pageable).map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryIdAndIsActiveTrue(categoryId).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getFeaturedProducts() {
        return productRepository.findByIsFeaturedTrueAndIsActiveTrue().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setIsActive(false);
        productRepository.save(product);
    }

    public void updateVariantStock(Long variantId, Integer quantity) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant not found with id: " + variantId));
        int newStock = variant.getStockQuantity() - quantity;
        if (newStock < 0) {
            throw new BadRequestException("Insufficient stock for product: " + variant.getProduct().getName());
        }
        variant.setStockQuantity(newStock);
        productVariantRepository.save(variant);
    }

    public void restoreVariantStock(Long variantId, Integer quantity) {
        if (quantity <= 0) {
            return;
        }
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant not found with id: " + variantId));
        variant.setStockQuantity(variant.getStockQuantity() + quantity);
        productVariantRepository.save(variant);
    }

    public void incrementViewCount(Long productId) {
        productRepository.findById(productId).ifPresent(p -> {
            if (p.getViewCount() == null) {
                p.setViewCount(1L);
            } else {
                p.setViewCount(p.getViewCount() + 1);
            }
            productRepository.save(p);
        });
    }

    // ====================== Variant CRUD ======================
    public ProductVariantDTO addVariantToProduct(Long productId, ProductVariantDTO variantDTO) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (variantDTO.getSku() == null || variantDTO.getSku().isBlank()) {
            throw new BadRequestException("SKU is required for variant.");
        }
        if (productVariantRepository.existsBySku(variantDTO.getSku())) {
            throw new BadRequestException("SKU '" + variantDTO.getSku() + "' already exists.");
        }

        ProductVariant variant = new ProductVariant();
        mapVariantDtoToEntity(variantDTO, variant);
        variant.setProduct(product);
        ProductVariant savedVariant = productVariantRepository.save(variant);

        if (product.getVariants() == null) {
            product.setVariants(new ArrayList<>());
        }
        product.getVariants().add(savedVariant);
        productRepository.save(product);

        return convertVariantToDTO(savedVariant);
    }

    public ProductVariantDTO updateVariant(Long productId, Long variantId, ProductVariantDTO variantDTO) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        ProductVariant existingVariant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));

        if (!existingVariant.getProduct().getId().equals(product.getId())) {
            throw new BadRequestException("Variant does not belong to the specified product.");
        }

        if (variantDTO.getSku() == null || variantDTO.getSku().isBlank()) {
            throw new BadRequestException("SKU is required for variant.");
        }
        Optional<ProductVariant> bySku = productVariantRepository.findBySku(variantDTO.getSku());
        if (bySku.isPresent() && !bySku.get().getId().equals(variantId)) {
            throw new BadRequestException("SKU '" + variantDTO.getSku() + "' is already in use by another variant.");
        }

        mapVariantDtoToEntity(variantDTO, existingVariant);
        existingVariant.setProduct(product);

        ProductVariant saved = productVariantRepository.save(existingVariant);

        if (product.getVariants() == null) {
            product.setVariants(new ArrayList<>());
        }
        product.getVariants().removeIf(v -> v.getId().equals(saved.getId()));
        product.getVariants().add(saved);
        productRepository.save(product);

        return convertVariantToDTO(saved);
    }

    public void deleteVariant(Long productId, Long variantId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));

        if (!variant.getProduct().getId().equals(product.getId())) {
            throw new BadRequestException("Variant does not belong to the specified product.");
        }

        if (product.getVariants() != null) {
            product.getVariants().removeIf(v -> v.getId().equals(variantId));
            productRepository.save(product);
        }

        productVariantRepository.delete(variant);
    }

    // ====================== Mappers ======================
    private void mapDtoToEntity(ProductDTO dto, Product product) {
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setImageUrl(dto.getImageUrl());
        product.setSpecifications(dto.getSpecifications());
        if (dto.getIsFeatured() != null) {
            product.setIsFeatured(dto.getIsFeatured());
        }
    }

    private void mapVariantDtoToEntity(ProductVariantDTO dto, ProductVariant entity) {
        entity.setColor(dto.getColor());
        entity.setColorImageUrl(dto.getColorImageUrl());
        entity.setProductSize(dto.getProductSize());
        entity.setSku(dto.getSku());
        entity.setStockQuantity(dto.getStockQuantity());
        entity.setPrice(dto.getPrice() != null && dto.getPrice().compareTo(BigDecimal.ZERO) > 0 ? dto.getPrice() : null);
        entity.setImageUrl(dto.getImageUrl());
    }

    public ProductDTO convertToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategoryName(product.getCategory().getName());
        }
        dto.setImageUrl(product.getImageUrl());
        dto.setIsActive(product.getIsActive());
        dto.setAverageRating(product.getAverageRating());
        dto.setReviewCount(product.getReviewCount());
        dto.setSpecifications(product.getSpecifications());
        dto.setIsFeatured(product.getIsFeatured());
        dto.setViewCount(product.getViewCount());

        if (product.getVariants() != null) {
            dto.setVariants(product.getVariants().stream().map(this::convertVariantToDTO).collect(Collectors.toList()));
            dto.setStockQuantity(product.getVariants().stream().mapToInt(ProductVariant::getStockQuantity).sum());
        } else {
            dto.setVariants(new ArrayList<>());
            dto.setStockQuantity(0);
        }
        return dto;
    }

    private ProductVariantDTO convertVariantToDTO(ProductVariant variant) {
        ProductVariantDTO dto = new ProductVariantDTO();
        dto.setId(variant.getId());
        dto.setProductId(variant.getProduct() != null ? variant.getProduct().getId() : null);
        dto.setColor(variant.getColor());
        dto.setColorImageUrl(variant.getColorImageUrl());
        dto.setProductSize(variant.getProductSize());
        dto.setSku(variant.getSku());
        dto.setStockQuantity(variant.getStockQuantity());
        dto.setPrice(variant.getPrice());
        dto.setImageUrl(variant.getImageUrl());
        return dto;
    }
}
