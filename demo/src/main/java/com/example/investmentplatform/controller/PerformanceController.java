package com.example.investmentplatform.controller;

import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.investmentplatform.entity.Portfolio;
import com.example.investmentplatform.exceptions.ResourceNotFoundException;
import com.example.investmentplatform.repository.PortfolioRepository;
import com.example.investmentplatform.service.PortfolioPerformanceService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
public class PerformanceController {
    
    private final PortfolioPerformanceService performanceService;
    private final PortfolioRepository portfolioRepository;
    
    @GetMapping("/portfolio/{id}")
    public ResponseEntity<PerformanceResponse> getPerformance(@PathVariable Long id) {
        Portfolio portfolio = portfolioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found"));
            
        return ResponseEntity.ok(new PerformanceResponse(
            portfolio.getPerformanceScore(),
            portfolio.getRiskAdjustedReturn(),
            portfolio.getLastCalculated()
        ));
    }
    
    @Data
    @AllArgsConstructor
    public static class PerformanceResponse {
        private Double performanceScore;
        private Double riskAdjustedReturn;
        private LocalDateTime lastUpdated;
    }
}