package com.example.investmentplatform.controller;

import com.example.investmentplatform.entity.Portfolio;
import com.example.investmentplatform.repository.PortfolioRepository;
import com.example.investmentplatform.repository.RecommendationFeedbackRepository;
import com.example.investmentplatform.service.RecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeedbackController.class)
@ActiveProfiles("test")
public class FeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortfolioRepository portfolioRepository;

    @MockBean
    private RecommendationFeedbackRepository feedbackRepository;

    @MockBean
    private RecommendationService recommendationService;

    @Test
    public void submitFeedback_Success() throws Exception {
        when(portfolioRepository.findById(1L)).thenReturn(Optional.of(new Portfolio()));

        mockMvc.perform(post("/api/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"portfolioId\":1,\"rating\":5,\"comments\":\"Great\",\"appliedChanges\":true}"))
                .andExpect(status().isCreated());
    }

    @Test
    public void submitFeedback_PortfolioNotFound() throws Exception {
        when(portfolioRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"portfolioId\":1}"))
                .andExpect(status().isNotFound());
    }
}
