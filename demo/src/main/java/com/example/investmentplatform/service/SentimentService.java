package com.example.investmentplatform.service;

import java.util.List;
import java.util.Map;
import java.util.logging.*; 
import java.util.stream.Collectors;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.investmentplatform.entity.SentimentHistory;
import com.example.investmentplatform.repository.SentimentHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SentimentService {
    
    @Autowired
    private SentimentHistoryRepository sentimentRepo;

    @Autowired
    private RestTemplate restTemplate;

    private static final double DEFAULT_SENTIMENT = 0.5;
    private static final double MIN_STD_DEV = 0.001;
    private static final double ABSOLUTE_THRESHOLD = 0.1;
    private static final double WEIGHT_DECAY_FACTOR = 0.9;

    public List<SentimentHistory> getSentimentData(String stockSymbol) {
        return sentimentRepo.findByStockSymbolOrderByTimestampDesc(stockSymbol);
    }

    public Map<String, Double> getSentimentSummary(String stockSymbol) {
        List<SentimentHistory> history = sentimentRepo.findByStockSymbol(stockSymbol);

        if (history.isEmpty()) {
            log.warn("No sentiment data found for: " + stockSymbol);
            return Map.of();  // Return an empty map instead of null
        }
        
        return history.stream()
        .collect(Collectors.groupingBy(
            entry -> entry.getTimestamp().toLocalDate().toString(),
            Collectors.averagingDouble(SentimentHistory::getSentimentScore)
        ));
    }


    public double analyzeStockSentiment(String stockSymbol) {
        try {
            String url = "http://localhost:5001/sentiment/" + stockSymbol;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> body = response.getBody();

            if (body != null && body.containsKey("sentiment_score")) {
                return Double.parseDouble(body.get("sentiment_score").toString());
            } else {
                return 0.5;
            }
        } catch (Exception e) {
            Logger.getLogger(SentimentService.class.getName()).warning("Sentiment API failed: " + e.getMessage());
            return 0.5;
        }
    }

    public boolean isSignificantSentimentShift(String stockSymbol, double thresholdZScore, int windowDays) {
        // Fetch recent data (last 7 days)
        LocalDateTime cutoff = LocalDateTime.now().minusDays(windowDays);
        List<SentimentHistory> history = sentimentRepo.findRecentBySymbol(stockSymbol, cutoff);

        if (history.size() < 5) {
            log.debug("Insufficient data for {} ({} records)", stockSymbol, history.size());
            return false;
        }

        SentimentStats stats = calculateStats(history);
        double zScore = calculateZScore(history.get(0).getSentimentScore(), stats);

        log.debug("{} stats: μ={:.2f}, σ={:.2f}, z={:.2f}", 
                 stockSymbol, stats.movingAvg(), stats.stdDev(), zScore);

        return Math.abs(zScore) > thresholdZScore;
    }

    public void analyzeAndSaveSentiment(String stockSymbol) {

        Map<String, Object> sentimentData = fetchSentimentDetails(stockSymbol);
        
        double currentScore = Double.parseDouble(sentimentData.get("sentiment_score").toString());
        String title = sentimentData.getOrDefault("title", "N/A").toString();
        String content = sentimentData.getOrDefault("content", "N/A").toString();
        
        if (title.equals("N/A") || content.equals("N/A")) {
            log.warn("Skipping DB insert for {} due to missing title/content", stockSymbol);
            return;
        }
        List<SentimentHistory> history = sentimentRepo.findTop10ByStockSymbolOrderByTimestampDesc(stockSymbol);
        SentimentStats stats = calculateStats(history);

        SentimentHistory newEntry = SentimentHistory.builder()
            .stockSymbol(stockSymbol)
            .timestamp(LocalDateTime.now())
            .sentimentScore(currentScore)
            .movingAvg(stats.movingAvg())
            .stdDev(stats.stdDev())
            .title(title)
            .content(content)
            .build();
            
        sentimentRepo.save(newEntry);
    }

    private Map<String, Object> fetchSentimentDetails(String stockSymbol) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                "http://localhost:5001/sentiment/" + stockSymbol, Map.class);
    
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody(); // full dict: title, content, sentiment_score, etc.
            }
        } catch (Exception e) {
            log.error("Failed to fetch sentiment for {}: {}", stockSymbol, e.getMessage());
        }
        return Map.of("sentiment_score", DEFAULT_SENTIMENT, "title", "N/A", "content", "N/A");
    }
    
    
    private SentimentStats calculateStats(List<SentimentHistory> history) {
        if (history.isEmpty()) return new SentimentStats(DEFAULT_SENTIMENT, 0.0);

        // Weighted average calculation
        double sum = 0;
        double sumWeights = 0;
        for (int i = 0; i < history.size(); i++) {
            double weight = Math.pow(WEIGHT_DECAY_FACTOR, i);
            sum += history.get(i).getSentimentScore() * weight;
            sumWeights += weight;
        }
        double movingAvg = sum / sumWeights;

        // Weighted standard deviation
        double variance = 0;
        for (int i = 0; i < history.size(); i++) {
            double weight = Math.pow(WEIGHT_DECAY_FACTOR, i);
            variance += weight * Math.pow(history.get(i).getSentimentScore() - movingAvg, 2);
        }
        double stdDev = Math.sqrt(variance / sumWeights);

        return new SentimentStats(movingAvg, stdDev);
    }

    private double calculateZScore(double currentScore, SentimentStats stats) {
        if (stats.stdDev() < MIN_STD_DEV) {
            return (Math.abs(currentScore - stats.movingAvg()) > ABSOLUTE_THRESHOLD) ? 
                   Double.MAX_VALUE : 0;
        }
        return (currentScore - stats.movingAvg()) / stats.stdDev();
    }
    // Helper record
    private record SentimentStats(double movingAvg, double stdDev) {}

}
