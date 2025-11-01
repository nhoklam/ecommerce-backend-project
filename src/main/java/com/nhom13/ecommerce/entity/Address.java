package com.nhom13.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "addresses")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Address extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String phone;
    
    @Column(nullable = false)
    private String street;

    @Column(nullable = false)
    private String city; // (Thành phố/Tỉnh)

    @Column(nullable = false)
    private String district; // (Quận/Huyện)

    @Column(nullable = false)
    private String ward; // (Phường/Xã)

    @Column(nullable = false)
    private Boolean isDefault = false;

    // Thêm phương thức helper
    public String getFullAddress() {
        return String.format("%s, %s, %s, %s", street, ward, district, city);
    }
}