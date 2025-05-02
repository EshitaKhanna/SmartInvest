package com.example.investmentplatform.service;

import com.example.investmentplatform.entity.Portfolio;
import com.example.investmentplatform.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioPerformanceService {
    private final PortfolioRepository portfolioRepository;
    private final RestTemplate restTemplate;

    @Value("${python.service.url:http://localhost:5001}")
    private String pythonServiceBaseUrl;

    @Scheduled(cron = "${portfolio.performance.cron:0 0 18 * * MON-FRI}") // Mon-Fri 6PM
    public void calculateDailyPerformance() {
        log.info("Starting daily portfolio performance calculation");
        long startTime = System.nanoTime();

        portfolioRepository.findAll().forEach(portfolio -> {
            try {
                calculateAndSavePerformance(portfolio);
            } catch (Exception e) {
                log.error("Failed to calculate performance for portfolio {}: {}", 
                          portfolio.getId(), e.getMessage());
            }
        });

        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        log.info("Completed performance calculations in {} ms", duration);
    }

    private void calculateAndSavePerformance(Portfolio portfolio) {
        Map<String, Double> performanceMetrics = fetchPerformanceMetrics();

        portfolio.setPerformanceScore(performanceMetrics.get("performanceScore"));
        portfolio.setRiskAdjustedReturn(performanceMetrics.get("riskAdjustedReturn"));
        portfolio.setLastCalculated(LocalDateTime.now());

        portfolioRepository.save(portfolio);
        log.debug("Updated performance for portfolio {}", portfolio.getId());
    }

    private Map<String, Double> fetchPerformanceMetrics() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                pythonServiceBaseUrl + "/api/performance", Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Double> metrics = response.getBody();

                double portfolioReturn = metrics.get("portfolioReturn");
                double portfolioVolatility = metrics.get("portfolioVolatility");

                double benchmarkReturn = fetchBenchmarkReturn();
                double alpha = portfolioReturn - benchmarkReturn;
                double sharpeRatio = calculateSharpeRatio(portfolioReturn, portfolioVolatility);

                return Map.of(
                    "performanceScore", alpha,
                    "riskAdjustedReturn", sharpeRatio
                );
            }
        } catch (Exception e) {
            log.warn("Failed to fetch metrics from Python service: {}", e.getMessage());
        }

        return Map.of(
            "performanceScore", 0.0,
            "riskAdjustedReturn", 0.0
        );
    }

    private double fetchBenchmarkReturn() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                pythonServiceBaseUrl + "/api/market-return?symbol=^GSPC", Map.class
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Double.parseDouble(response.getBody().get("return").toString());
            }
        } catch (Exception e) {
            log.error("Failed to fetch benchmark return: {}", e.getMessage());
        }
        return 0.0;
    }

    private double calculateSharpeRatio(double returns, double volatility) {
        if (volatility == 0) return 0.0;
        return returns / volatility; // Risk-free rate assumed 0
    }

    public void calculatePortfolioPerformance(Long portfolioId) {
        portfolioRepository.findById(portfolioId).ifPresent(portfolio -> {
            calculateAndSavePerformance(portfolio);
            log.info("Manually calculated performance for portfolio {}", portfolioId);
        });
    }
}
