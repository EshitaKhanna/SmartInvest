package com.example.investmentplatform.service;

import java.util.logging.Logger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;

import com.example.investmentplatform.entity.Portfolio;
import com.example.investmentplatform.entity.User;
import com.example.investmentplatform.exceptions.RecommendationException;
import com.example.investmentplatform.repository.RecommendationFeedbackRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RequiredArgsConstructor
@Slf4j
@Service
public class RecommendationService {


    private final RecommendationFeedbackRepository feedbackRepository;
    @Lazy
    private final ModelRetrainingService retrainingService;
    private final RestTemplate restTemplate;

    private static final Logger logger = Logger.getLogger(RecommendationService.class.getName());

    @CircuitBreaker(name = "recommendationService", fallbackMethod = "getFallbackAllocation")
    @TimeLimiter(name = "recommendationService")
    public CompletableFuture<Map<String, Double>> getPersonalizedRecommendation(User user, Portfolio portfolio, double sentimentScore) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, Double> currentAlloc = portfolio.getAllocation();
                
                Map<String, Object> requestPayload = Map.of(
                    "age", Period.between(user.getDob(), LocalDate.now()).getYears(),
                    "annual_income", user.getAnnualIncome(),
                    "risk_tolerance", user.getRiskTolerance().toString().toLowerCase(),
                    "investment_goal", user.getMainInvestmentGoal().toString().toLowerCase(),
                    "stocks_current", getPercent(currentAlloc, "stocks"),
                    "bonds_current", getPercent(currentAlloc, "bonds"),
                    "crypto_current", getPercent(currentAlloc, "crypto"),
                    "sentiment_score", sentimentScore
                );

                ResponseEntity<Map> response = restTemplate.exchange(
                    "http://localhost:5001/recommend",
                    HttpMethod.POST,
                    new HttpEntity<>(requestPayload, headers),
                    Map.class
                );

                return (Map<String, Double>) response.getBody();

            } catch (Exception e) {
                log.error("🚨 Recommendation failed for user {}: {}", user.getId(), e.getMessage(), e);
                throw new RecommendationException("Failed to generate recommendation");
            }
        });
    }

    // Get a specific asset class weight safely from allocation map.
    private double getPercent(Map<String, Double> map, String key) {
        if (map == null || !map.containsKey(key)) return 0.0;
    
        double total = map.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total <= 0) return 0.0;
    
        return map.get(key) / total;
    }

    // Retraining logic based on feedback volume (invoked manually or by scheduler).
    public void checkForRetraining() {
            long newFeedbackCount = feedbackRepository.countByTimestampAfter(
                LocalDateTime.now().minusWeeks(1));
            
            if (newFeedbackCount >= 100) { // Threshold for retraining
                log.info("Triggering retraining with {} new feedbacks", newFeedbackCount);
                retrainingService.retrainModelWithFeedback();
            }
    }

    // Fallback method triggered by CircuitBreaker.
    private CompletableFuture<Map<String, Double>> getFallbackAllocation(User user, Portfolio portfolio, double sentimentScore, Throwable ex) {
        log.warn("⚠️ Fallback triggered for user {} due to: {}", user.getId(), ex.getMessage());
        return CompletableFuture.completedFuture(Map.of(
            "stocks", 0.6,
            "bonds", 0.3,
            "crypto", 0.1
        ));
    }

    // scheduled retraining every Monday at 3 AM.
    @Scheduled(cron = "0 0 3 * * MON") // Every Monday at 3 AM
    public void weeklyRetrainingCheck() {
        try {
            long newFeedbackCount = feedbackRepository.countByTimestampAfter(
                LocalDateTime.now().minusWeeks(1));
            
            if (newFeedbackCount >= 100) {
                log.info("Scheduled retraining with {} feedbacks", newFeedbackCount);
                retrainingService.retrainModelWithFeedback();
            }
        } catch (Exception e) {
            log.error("Scheduled retraining failed", e);
        }
    }

}
