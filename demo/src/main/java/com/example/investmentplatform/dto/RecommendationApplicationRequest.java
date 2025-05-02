package com.example.investmentplatform.dto;

import java.util.Map;

import jakarta.validation.constraints.NotNull;

public class RecommendationApplicationRequest {
    @NotNull
    private Map<String, Double> recommendation;
    
    private boolean recalculateDetailed = true;
    
    public Map<String, Double> getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(Map<String, Double> recommendation) {
        this.recommendation = recommendation;
    }

    public boolean isRecalculateDetailed() {
        return recalculateDetailed;
    }

    public void setRecalculateDetailed(boolean recalculateDetailed) {
        this.recalculateDetailed = recalculateDetailed;
    }

    // Validation method
    public boolean isValid() {
        if (recommendation == null) return false;
        double sum = recommendation.values().stream().mapToDouble(Double::doubleValue).sum();
        return Math.abs(sum - 1.0) < 0.0001; // Allow for floating point precision
    }
    

}