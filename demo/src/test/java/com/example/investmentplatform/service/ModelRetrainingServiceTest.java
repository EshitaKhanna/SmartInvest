package com.example.investmentplatform.service;

import com.example.investmentplatform.repository.RecommendationFeedbackRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelRetrainingServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RecommendationFeedbackRepository feedbackRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ModelRetrainingService modelRetrainingService;

    @Test
    void prepareTrainingData_Success() throws Exception {
        when(feedbackRepository.findRecentFeedbackForTraining(any()))
            .thenReturn(List.of(Map.of("rating", 5, "comments", "Great")));
        
        Map<String, Object> result = modelRetrainingService.prepareTrainingData();
        
        assertNotNull(result.get("feedback_data"));
        assertNotNull(result.get("metadata"));
    }
}