package com.example.investmentplatform.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.investmentplatform.entity.Portfolio;
import com.example.investmentplatform.exceptions.ResourceNotFoundException;
import com.example.investmentplatform.repository.PortfolioRepository;
import com.example.investmentplatform.entity.RecommendationFeedback;
import com.example.investmentplatform.repository.RecommendationFeedbackRepository;
import com.example.investmentplatform.service.RecommendationService;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import lombok.Data;
import lombok.RequiredArgsConstructor;


@RequestMapping("/api/feedback")
@RequiredArgsConstructor
@RestController
public class FeedbackController {
    
    private final RecommendationFeedbackRepository feedbackRepository;
    private final PortfolioRepository portfolioRepository;
    private final RecommendationService recommendationService;

    @PostMapping
    public ResponseEntity<?> submitFeedback(@RequestBody FeedbackRequest request) {
        Portfolio portfolio = portfolioRepository.findById(request.getPortfolioId())
            .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found"));
        
            RecommendationFeedback feedback = RecommendationFeedback.builder()
            .portfolio(portfolio)
            .rating(request.getRating())
            .comments(request.getComments())
            .appliedChanges(request.isAppliedChanges())
            .build();
        
        feedbackRepository.save(feedback);
        
        
        // Trigger model retraining if enough new feedback
        recommendationService.checkForRetraining();
        
        return ResponseEntity.status(HttpStatus.CREATED).body("Feedback submitted successfully");

    }

    @Data
    public static class FeedbackRequest {
        private Long portfolioId;
        @Min(1) @Max(5)
        private Integer rating;
        private String comments;
        private boolean appliedChanges;
    }
}