package com.example.investmentplatform.scheduler;

import com.example.investmentplatform.service.AlertService;
import com.example.investmentplatform.service.SentimentService;
import com.example.investmentplatform.service.WatchListService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class SentimentScheduler {

    private final WatchListService watchlistService;
    private final SentimentService sentimentService;
    private final AlertService alertService;

    private static final double Z_SCORE_THRESHOLD = 2.0;
    private static final int ANALYSIS_WINDOW_DAYS = 7;

    @Autowired
    public SentimentScheduler(WatchListService watchlistService, SentimentService sentimentService, AlertService alertService) {
        this.watchlistService = watchlistService;
        this.sentimentService = sentimentService;
        this.alertService = alertService;
    }


    @Scheduled(fixedRate = 3600000)
    public void checkSentimentShifts() {
        try {
            List<String> allSymbols = watchlistService.getAllWatchlistSymbols();
            log.info("Running sentiment check for {} symbols", allSymbols.size());

            allSymbols.parallelStream().forEach(symbol -> {
                try {
                    if (sentimentService.isSignificantSentimentShift(symbol, Z_SCORE_THRESHOLD, ANALYSIS_WINDOW_DAYS)) {
                        log.warn("Significant sentiment shift detected for: {}", symbol);
                        alertService.triggerSentimentAlert(symbol);
                    }
                } catch (Exception e) {
                    log.error("Error processing symbol {}: {}", symbol, e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Error in sentiment scheduler: {}", e.getMessage());
        }
    }

    @Scheduled(fixedRate = 3600000) // Every hour
    public void analyzeStocks() {
        watchlistService.getAllWatchlistSymbols()
            .forEach(symbol -> {
                try{
                    sentimentService.analyzeAndSaveSentiment(symbol);
                    log.info("Analyzed sentiment for {}", symbol);
                }catch (Exception e) {
                    log.error("Failed to analyze {}: {}", symbol, e.getMessage());
                }
                
            });
    }

}
