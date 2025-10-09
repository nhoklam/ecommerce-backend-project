package com.nhom13.ecommerce.dto;

import lombok.Data;
import java.util.List;

@Data
public class SearchResultDTO {
    private List<ProductDTO> products;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int size;
}