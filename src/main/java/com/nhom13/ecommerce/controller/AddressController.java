package com.nhom13.ecommerce.controller;

import com.nhom13.ecommerce.dto.AddressDTO;
import com.nhom13.ecommerce.dto.UserDTO;
import com.nhom13.ecommerce.service.AddressService;
import com.nhom13.ecommerce.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AddressController {

    private final AddressService addressService;
    private final UserService userService; // Để lấy userId

    private Long getUserId(Authentication authentication) {
        UserDTO user = userService.getUserByEmail(authentication.getName());
        return user.getId();
    }

    @PostMapping
    public ResponseEntity<AddressDTO> createAddress(@Valid @RequestBody AddressDTO dto, Authentication authentication) {
        AddressDTO createdAddress = addressService.createAddress(getUserId(authentication), dto);
        return new ResponseEntity<>(createdAddress, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AddressDTO>> getMyAddresses(Authentication authentication) {
        List<AddressDTO> addresses = addressService.getAddressesByUserId(getUserId(authentication));
        return ResponseEntity.ok(addresses);
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<AddressDTO> updateAddress(
            @PathVariable Long addressId,
            @Valid @RequestBody AddressDTO dto,
            Authentication authentication) {
        
        AddressDTO updatedAddress = addressService.updateAddress(addressId, getUserId(authentication), dto);
        return ResponseEntity.ok(updatedAddress);
    }

    @PutMapping("/{addressId}/default")
    public ResponseEntity<Void> setDefaultAddress(@PathVariable Long addressId, Authentication authentication) {
        addressService.setDefaultAddress(addressId, getUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long addressId, Authentication authentication) {
        addressService.deleteAddress(addressId, getUserId(authentication));
        return ResponseEntity.noContent().build();
    }
}