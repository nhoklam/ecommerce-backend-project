package com.nhom13.ecommerce.service;

import com.nhom13.ecommerce.dto.AddressDTO;
import com.nhom13.ecommerce.entity.Address;
import com.nhom13.ecommerce.entity.User;
import com.nhom13.ecommerce.exception.BadRequestException;
import com.nhom13.ecommerce.exception.ResourceNotFoundException;
import com.nhom13.ecommerce.repository.AddressRepository;
import com.nhom13.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressDTO createAddress(Long userId, AddressDTO dto) {
        User user = userRepository.findById(userId)
           .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Address address = new Address();
        mapDtoToEntity(dto, address);
        address.setUser(user);

        if (dto.getIsDefault()!= null && dto.getIsDefault()) {
            addressRepository.resetDefaultAddress(userId);
            address.setIsDefault(true);
        }

        Address savedAddress = addressRepository.save(address);
        return convertToDTO(savedAddress);
    }

    @Transactional(readOnly = true)
    public List<AddressDTO> getAddressesByUserId(Long userId) {
        return addressRepository.findByUserId(userId).stream()
           .map(this::convertToDTO)
           .collect(Collectors.toList());
    }

    public AddressDTO updateAddress(Long addressId, Long userId, AddressDTO dto) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
           .orElseThrow(() -> new ResourceNotFoundException("Address not found or does not belong to user"));

        mapDtoToEntity(dto, address);

        if (dto.getIsDefault()!= null && dto.getIsDefault()) {
            addressRepository.resetDefaultAddress(userId);
            address.setIsDefault(true);
        }

        Address updatedAddress = addressRepository.save(address);
        return convertToDTO(updatedAddress);
    }

    public void deleteAddress(Long addressId, Long userId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
           .orElseThrow(() -> new ResourceNotFoundException("Address not found or does not belong to user"));
        
        if(address.getIsDefault()){
            throw new BadRequestException("Cannot delete default address. Please set another address as default first.");
        }

        addressRepository.delete(address);
    }

    @Transactional
    public void setDefaultAddress(Long addressId, Long userId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
           .orElseThrow(() -> new ResourceNotFoundException("Address not found or does not belong to user"));

        addressRepository.resetDefaultAddress(userId);
        address.setIsDefault(true);
        addressRepository.save(address);
    }

    // Mappers
    private AddressDTO convertToDTO(Address entity) {
        AddressDTO dto = new AddressDTO();
        dto.setId(entity.getId());
        dto.setFullName(entity.getFullName());
        dto.setPhone(entity.getPhone());
        dto.setStreet(entity.getStreet());
        dto.setCity(entity.getCity());
        dto.setDistrict(entity.getDistrict());
        dto.setWard(entity.getWard());
        dto.setIsDefault(entity.getIsDefault());
        return dto;
    }

    private void mapDtoToEntity(AddressDTO dto, Address entity) {
        entity.setFullName(dto.getFullName());
        entity.setPhone(dto.getPhone());
        entity.setStreet(dto.getStreet());
        entity.setCity(dto.getCity());
        entity.setDistrict(dto.getDistrict());
        entity.setWard(dto.getWard());
        if (dto.getIsDefault()!= null) {
            entity.setIsDefault(dto.getIsDefault());
        }
    }
}