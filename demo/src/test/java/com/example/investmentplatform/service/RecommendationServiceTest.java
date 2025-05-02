package com.example.investmentplatform.service;

import com.example.investmentplatform.entity.Portfolio;
import com.example.investmentplatform.entity.User;
import com.example.investmentplatform.enums.InvestmentGoal;
import com.example.investmentplatform.enums.RiskTolerance;
import com.example.investmentplatform.exceptions.RecommendationException;
import com.example.investmentplatform.repository.RecommendationFeedbackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RecommendationFeedbackRepository feedbackRepository;

    @Mock
    private ModelRetrainingService retrainingService;

    @InjectMocks
    private RecommendationService recommendationService;

    private User testUser;
    private Portfolio testPortfolio;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setDob(LocalDate.of(1990, 1, 1));
        testUser.setRiskTolerance(RiskTolerance.MODERATE);
        testUser.setMainInvestmentGoal(InvestmentGoal.GROWTH);

        testPortfolio = new Portfolio();
        testPortfolio.setUser(testUser);
    }

    @Test
    void getPersonalizedRecommendation_Success() throws Exception {
        when(restTemplate.exchange(anyString(), any(), any(), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(Map.of("stocks", 0.7, "bonds", 0.2, "crypto", 0.1)));

        CompletableFuture<Map<String, Double>> future = recommendationService
            .getPersonalizedRecommendation(testUser, testPortfolio, 0.8);
        
        Map<String, Double> result = future.get();
        assertEquals(0.7, result.get("stocks"));
    }
}