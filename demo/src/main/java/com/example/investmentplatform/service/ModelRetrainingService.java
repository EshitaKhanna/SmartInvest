package com.example.investmentplatform.service;

import com.example.investmentplatform.exceptions.RetrainingException;
import com.example.investmentplatform.repository.RecommendationFeedbackRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Service
public class ModelRetrainingService {
    private final RestTemplate restTemplate;
    private final RecommendationFeedbackRepository feedbackRepository;
    private final ObjectMapper objectMapper;

    @Value("${ml.service.retrain.url}")
    private String retrainEndpoint;

    @Value("${ml.service.url}")
    private String mlServiceUrl;

    @Value("${ml.service.timeout:30000}")
    private int timeout;

    
    public void retrainModelWithFeedback() {
        try {
            // 1. Prepare feedback data for training
            Map<String, Object> trainingData = prepareTrainingData();

            if (trainingData.get("feedback_data") == null || 
                ((List<?>)trainingData.get("feedback_data")).size() < 50) {
                log.warn("Skipping retraining - insufficient data");
                return;
            }
            
            // 2. Call Python ML service
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> request = new HttpEntity<>(
                objectMapper.writeValueAsString(trainingData), 
                headers
            );

            restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
            ((HttpComponentsClientHttpRequestFactory)restTemplate.getRequestFactory())
                .setConnectTimeout(timeout);
            
                ResponseEntity<RetrainResponse> response = restTemplate.postForEntity(
                    retrainEndpoint,
                    request,
                    RetrainResponse.class
                );
    
            
                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    log.info("Model retrained successfully. Metrics: {}", response.getBody());
                } else {
                    throw new RetrainingException("Retraining failed with status: " + response.getStatusCode());
                }
            
        } catch (Exception e) {
            log.error("🚨 Error during model retraining: {}", e.getMessage());
        }
    }

    Map<String, Object> prepareTrainingData() {
        Map<String, Object> data = new HashMap<>();
        
        // Add recent feedback (last 3 months)
        data.put("feedback_data", feedbackRepository.findRecentFeedbackForTraining(
            LocalDateTime.now().minusMonths(3)
        ));
        
        // Add metadata
        data.put("metadata", Map.of(
            "timestamp", LocalDateTime.now().toString(),
            "version", "1.2",
            "environment", System.getProperty("spring.profiles.active", "default")
        ));
        
        return data;
    }

    private boolean validateRetrainingData(Map<String, Object> trainingData) {
        if (trainingData == null || trainingData.isEmpty()) {
            log.warn("No training data available");
            return false;
        }
        
        List<?> feedbackData = (List<?>) trainingData.get("feedback_data");
        return feedbackData != null && feedbackData.size() >= 10; // Minimum samples
    }
    
    @Data
    public static class RetrainResponse {
        private String status;
        private String modelVersion;
        private Map<String, Double> metrics;
    }


}