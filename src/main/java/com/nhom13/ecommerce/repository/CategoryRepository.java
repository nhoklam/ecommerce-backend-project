package com.nhom13.ecommerce.repository;

import com.nhom13.ecommerce.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    @Query("SELECT c FROM Category c WHERE c.isActive = true")
    List<Category> findAllActiveCategories();
    
    List<Category> findByNameContainingIgnoreCase(String name);
}