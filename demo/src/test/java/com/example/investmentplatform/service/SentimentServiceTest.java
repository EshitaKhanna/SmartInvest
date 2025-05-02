package com.example.investmentplatform.service;

import com.example.investmentplatform.entity.SentimentHistory;
import com.example.investmentplatform.repository.SentimentHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SentimentServiceTest {

    @Mock
    private SentimentHistoryRepository sentimentRepo;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private SentimentService sentimentService;

    @Test
    void analyzeStockSentiment_Success() {
        when(restTemplate.getForEntity(anyString(), any()))
            .thenReturn(ResponseEntity.ok(Map.of("sentiment_score", 0.75)));

        double result = sentimentService.analyzeStockSentiment("AAPL");
        assertEquals(0.75, result);
    }
}