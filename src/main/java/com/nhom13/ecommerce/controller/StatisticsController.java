package com.nhom13.ecommerce.controller;

import com.nhom13.ecommerce.dto.StatisticsDTO;
import com.nhom13.ecommerce.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class StatisticsController {
    
    private final StatisticsService statisticsService;
    
    @GetMapping
    public ResponseEntity<StatisticsDTO> getStatistics() {
        StatisticsDTO statistics = statisticsService.getStatistics();
        return ResponseEntity.ok(statistics);
    }
    
    @GetMapping("/monthly")
    public ResponseEntity<StatisticsDTO> getMonthlyStatistics(
            @RequestParam int year,
            @RequestParam int month) {
        StatisticsDTO statistics = statisticsService.getMonthlyStatistics(year, month);
        return ResponseEntity.ok(statistics);
    }
}