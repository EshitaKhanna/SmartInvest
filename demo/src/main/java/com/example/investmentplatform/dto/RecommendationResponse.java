package com.example.investmentplatform.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class RecommendationResponse {
    private String message;
    private Map<String, Double> currentAllocation;
    private Map<String, Double> recommendedAllocation;
    private LocalDateTime lastUpdated;
    private LocalDateTime generatedAt;

    // Constructor for recommendation responses
    public RecommendationResponse(Map<String, Double> currentAllocation, Map<String, Double> recommendedAllocation, String message) {
        this.currentAllocation = currentAllocation;
        this.recommendedAllocation = recommendedAllocation;
        this.message = message;
        this.generatedAt = LocalDateTime.now();
    }

    // Constructor for error messages
    public RecommendationResponse(String errorMessage) {
    this.message = errorMessage;
    this.generatedAt = LocalDateTime.now();
    }

    // Constructor with lastUpdated timestamp
    public RecommendationResponse(Map<String, Double> currentAllocation,Map<String, Double> recommendedAllocation,
        String message,
        LocalDateTime lastUpdated) {
    this(currentAllocation, recommendedAllocation, message);
    this.lastUpdated = lastUpdated;
    }
    
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Double> getCurrentAllocation() {
        return currentAllocation;
    }

    public void setCurrentAllocation(Map<String, Double> currentAllocation) {
        this.currentAllocation = currentAllocation;
    }

    public Map<String, Double> getRecommendedAllocation() {
        return recommendedAllocation;
    }

    public void setRecommendedAllocation(Map<String, Double> recommendedAllocation) {
        this.recommendedAllocation = recommendedAllocation;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    
    
    // Getters
}
