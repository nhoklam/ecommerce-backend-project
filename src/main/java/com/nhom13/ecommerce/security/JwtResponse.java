package com.nhom13.ecommerce.security;

import com.nhom13.ecommerce.dto.UserDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private UserDTO user;
    
    public JwtResponse(String accessToken, UserDTO user) {
        this.token = accessToken;
        this.user = user;
    }
}