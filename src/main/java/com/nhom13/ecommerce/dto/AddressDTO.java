package com.nhom13.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressDTO {
    private Long id;
    
    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    private String phone;
    
    @NotBlank(message = "Street is required")
    private String street;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "District is required")
    private String district;

    @NotBlank(message = "Ward is required")
    private String ward;

    private Boolean isDefault;

    // Helper để tạo chuỗi địa chỉ đầy đủ
    public String getFullAddress() {
        return String.format("%s, %s, %s, %s", street, ward, district, city);
    }
}