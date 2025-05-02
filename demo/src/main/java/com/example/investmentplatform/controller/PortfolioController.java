package com.example.investmentplatform.controller;

import com.example.investmentplatform.dto.RecommendationApplicationRequest;
import com.example.investmentplatform.dto.RecommendationResponse;
import com.example.investmentplatform.entity.Portfolio;
import com.example.investmentplatform.entity.User;
import com.example.investmentplatform.exceptions.InvalidAllocationException;
import com.example.investmentplatform.exceptions.ResourceNotFoundException;
import com.example.investmentplatform.service.PortfolioService;
import com.example.investmentplatform.service.RecommendationService;
import com.example.investmentplatform.service.SentimentService;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.slf4j.Logger;

@RestController
@Slf4j
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private static final Logger logger = LoggerFactory.getLogger(PortfolioController.class);

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private SentimentService sentimentService;

    @Autowired
    private RestTemplate restTemplate; 

    @Autowired
    private RecommendationService recommendationService;

    @GetMapping("/user/{userId}")
    public List<Portfolio> getPortfoliosByUser(@PathVariable Long userId) {
        return portfolioService.getPortfoliosByUser(userId);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getPortfolioById(@PathVariable Long id) {
        Optional<Portfolio> portfolioOpt = portfolioService.getPortfolioById(id);
        if (portfolioOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(portfolioOpt.get());
    }

    @PostMapping
    public ResponseEntity<?> createPortfolio(@RequestBody Portfolio portfolio) {
        if (portfolio.getUser() == null || portfolio.getUser().getId() == null) {
            return ResponseEntity.badRequest().body("User ID is required");
        }
    
        Portfolio savedPortfolio = portfolioService.savePortfolio(portfolio, portfolio.getUser().getId());
        return ResponseEntity.ok(savedPortfolio);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePortfolio(@PathVariable Long id) {
        portfolioService.deletePortfolio(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/optimize/{id}") 
    public ResponseEntity<?> optimizePortfolio(@PathVariable Long id) throws JsonProcessingException {

        Optional<Portfolio> portfolioOpt = portfolioService.getPortfolioById(id);
        if (portfolioOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Portfolio not found");
        }

        Portfolio portfolio = portfolioOpt.get();
        User user = portfolio.getUser();

        // Fetch real-time sentiment scores
        Map<String, Double> sentimentScores = new HashMap<>();

        // Combine all symbols from all asset types
        List<String> allSymbols = new ArrayList<>();
        if (portfolio.getStocks() != null) allSymbols.addAll(portfolio.getStocks());
        if (portfolio.getBonds() != null) allSymbols.addAll(portfolio.getBonds());
        if (portfolio.getCrypto() != null) allSymbols.addAll(portfolio.getCrypto());


        for (String stockSymbol : allSymbols) {
            double score = sentimentService.analyzeStockSentiment(stockSymbol);
            sentimentScores.put(stockSymbol, score);
        }

        // Combine all asset symbols
        Map<String, List<String>> assets = Map.of(
        "stocks", portfolio.getStocks() != null ? portfolio.getStocks() : List.of(),
        "bonds", portfolio.getBonds() != null ? portfolio.getBonds() : List.of(),
        "crypto", portfolio.getCrypto() != null ? portfolio.getCrypto() : List.of()
        );

        // Enum-based risk value mapping
        double riskToleranceValue = switch (user.getRiskTolerance()) {
            case CONSERVATIVE -> 0.2;
            case AGGRESSIVE -> 0.8;
            default -> 0.5; // MODERATE
        };

        // Prepare request payload
        Map<String, Object> requestBodyMap = Map.of(
        "assets", assets,  // From portfolio's stocks/bonds/crypto
        "risk_tolerance", riskToleranceValue, // From user
        "investment_goal", user.getMainInvestmentGoal().toString().toLowerCase(), // From user
        "sentiment_scores", sentimentScores // sentiment scores
        );

        // Log the payload for debugging
        logger.info("Sending request to Flask: {}", requestBodyMap);

        // Convert payload to JSON
        ObjectMapper objectMapper = new ObjectMapper();
        String requestBody = objectMapper.writeValueAsString(requestBodyMap);

        // Set up HTTP headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        // Call Python optimization service
        String url = "http://localhost:5001/optimize";
        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

        // Process response
        Map<String, Object> responseBody = objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {});

        if (!responseBody.containsKey("optimized_weights")) {
            return ResponseEntity.badRequest().body("Error: Missing 'optimized_weights' in response");
        }

        // Convert and validate weights
        Map<String, Double> optimizedWeights = new HashMap<>();
        Object weightsObj = responseBody.get("optimized_weights");
        
        if (weightsObj instanceof Map<?, ?> tempMap) {
            tempMap.forEach((k, v) -> {
                if (k instanceof String symbol && v instanceof Number weight) {
                    optimizedWeights.put(symbol, weight.doubleValue());
                }
            });
        } else {
            return ResponseEntity.badRequest().body("Invalid weights format");
        }
        // Calculate high-level allocation (stocks, bonds, crypto)
        Map<String, Double> highLevelAllocation = new HashMap<>();
        double stocksRatio = 0.0, bondsRatio = 0.0, cryptoRatio = 0.0;

        for (Map.Entry<String, Double> entry : optimizedWeights.entrySet()) {
            String symbol = entry.getKey();
            double weight = entry.getValue();

            if (symbol.endsWith("-USD")  || portfolio.getCrypto().contains(symbol)) {  // Crypto symbol
                cryptoRatio += weight;
            } else if (symbol.startsWith("^") || portfolio.getBonds().contains(symbol) ) {  // Bond symbol
                bondsRatio += weight;
            } else {  // Stock symbol
                stocksRatio += weight;
            }
        }

        highLevelAllocation.put("stocks", stocksRatio);
        highLevelAllocation.put("bonds", bondsRatio);
        highLevelAllocation.put("crypto", cryptoRatio);

        // Update portfolio with both allocations
        portfolio.setAllocation(highLevelAllocation);  // High-level allocation
        portfolio.setDetailedAllocation(optimizedWeights);  // Detailed allocation

        // save the portfolio
        portfolioService.savePortfolio(portfolio, user.getId());

        // Return both allocations in the response
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("high_level_allocation", highLevelAllocation);
        responseMap.put("detailed_allocation", optimizedWeights);

        return ResponseEntity.ok(responseMap);
    }
   
    @PostMapping("/{id}/add-asset")
    public ResponseEntity<?> addAsset(
        @PathVariable Long id,
        @RequestParam String assetType,  // "stocks", "bonds", or "crypto"
        @RequestParam String symbol,     // Asset symbol (e.g., "AAPL", "BND", "BTC-USD")
        @RequestParam(required = false) Double weight  // Optional: weight of the asset
    ){
        Optional<Portfolio> portfolioOpt = portfolioService.getPortfolioById(id);
        if (portfolioOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Portfolio not found");
        }

        Portfolio portfolio = portfolioOpt.get();

        // Add asset to the appropriate list
        switch (assetType.toLowerCase()) {
            case "stocks":
                portfolio.getStocks().add(symbol);
                break;
            case "bonds":
                portfolio.getBonds().add(symbol);
                break;
            case "crypto":
                portfolio.getCrypto().add(symbol);
                break;
            default:
                return ResponseEntity.badRequest().body("Invalid asset type");
        }

        // Update weights if provided
        if (weight != null) {
            Map<String, Double> allocation = portfolio.getAllocation();
            allocation.put(symbol, weight);
            portfolio.setAllocation(allocation);
        }

        portfolioService.savePortfolio(portfolio, portfolio.getUser().getId());
        return ResponseEntity.ok("Asset added successfully");
    }

    @DeleteMapping("/{id}/delete-asset")
    public ResponseEntity<?> deleteAsset(
        @PathVariable Long id,
        @RequestParam String assetType,  // "stocks", "bonds", or "crypto"
        @RequestParam String symbol      // Asset symbol (e.g., "AAPL", "BND", "BTC-USD")
    ) {
        Optional<Portfolio> portfolioOpt = portfolioService.getPortfolioById(id);
        if (portfolioOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Portfolio not found");
        }

        Portfolio portfolio = portfolioOpt.get();

        // Remove asset from the appropriate list
        switch (assetType.toLowerCase()) {
            case "stocks":
                portfolio.getStocks().remove(symbol);
                break;
            case "bonds":
                portfolio.getBonds().remove(symbol);
                break;
            case "crypto":
                portfolio.getCrypto().remove(symbol);
                break;
            default:
                return ResponseEntity.badRequest().body("Invalid asset type");
        }

        // Remove weight from allocation
        Map<String, Double> allocation = portfolio.getAllocation();
        allocation.remove(symbol);
        portfolio.setAllocation(allocation);

        portfolioService.savePortfolio(portfolio, portfolio.getUser().getId());
        return ResponseEntity.ok("Asset deleted successfully");
    }

    @PutMapping("/{id}/update-weight")
    public ResponseEntity<?> updateWeight(
        @PathVariable Long id,
        @RequestParam String symbol,  // Asset symbol (e.g., "AAPL", "BND", "BTC-USD")
        @RequestParam Double weight   // New weight for the asset
    ) {
        Optional<Portfolio> portfolioOpt = portfolioService.getPortfolioById(id);
        if (portfolioOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Portfolio not found");
        }

        Portfolio portfolio = portfolioOpt.get();

        // Update weight in allocation
        Map<String, Double> allocation = portfolio.getAllocation();
        allocation.put(symbol, weight);
        portfolio.setAllocation(allocation);

        portfolioService.savePortfolio(portfolio, portfolio.getUser().getId());
        return ResponseEntity.ok("Weight updated successfully");
    }
    
        @GetMapping("/total-value/{userId}")
        public ResponseEntity<?> getTotalPortfolioValue(
            @PathVariable Long userId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
        ) {
            try {
                // 1. Verify JWT existence
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Missing or invalid Authorization header");
                }

                // 2. Get portfolios
                List<Portfolio> portfolios = portfolioService.getPortfoliosByUser(userId);
                if (portfolios.isEmpty()) {
                    return ResponseEntity.ok(0.0);
                }

                // 3. Prepare request
                double baseAmount = portfolios.get(0).getUser().getInvestmentAmount();
                List<Map<String, Object>> portfolioDetails = portfolios.stream()
                    .filter(p -> p.getDetailedAllocation() != null)
                    .map(p -> Map.<String, Object>of("detailedAllocation", (Object) p.getDetailedAllocation()))
                    .collect(Collectors.toList());

                Map<String, Object> requestBody = Map.of(
                    "portfolios", portfolioDetails,
                    "base_amount", baseAmount
                );

                // 4. Prepare headers with JWT
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set(HttpHeaders.AUTHORIZATION, authHeader);

                // 5. Make request with error handling
                try {
                    ResponseEntity<String> rawResponse = restTemplate.exchange(
                        "http://localhost:5001/portfolio/total-value",
                        HttpMethod.POST,
                        new HttpEntity<>(requestBody, headers),
                        String.class
                    );

                    return processFlaskResponse(rawResponse);
                } catch (HttpStatusCodeException e) {
                    log.error("Flask error response: {}", e.getResponseBodyAsString());
                    return ResponseEntity.status(e.getStatusCode())
                        .body("Portfolio service error: " + cleanErrorResponse(e.getResponseBodyAsString()));
                }
                
            } catch (ResourceNotFoundException e) {
                return ResponseEntity.notFound().build();
            } catch (Exception e) {
                log.error("Calculation error", e);
                return ResponseEntity.internalServerError()
                    .body("Error calculating portfolio value: " + e.getMessage());
            }
        }

        private ResponseEntity<?> processFlaskResponse(ResponseEntity<String> response) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.getBody());
                
                if (root.has("error")) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(root.get("error").asText());
                }
                
                JsonNode valueNode = root.get("value");
                if (valueNode.isDouble() || valueNode.isInt()) {
                    return ResponseEntity.ok(valueNode.asDouble());
                }
                
                // Handle NaN case
                if ("NaN".equalsIgnoreCase(valueNode.asText())) {
                    log.error("Invalid numeric value received from Flask");
                    return ResponseEntity.internalServerError()
                        .body("Invalid portfolio value calculation");
                }
                
                return ResponseEntity.ok(0.0);
                
            } catch (IOException e) {
                log.error("Failed to parse Flask response: {}", response.getBody());
                return ResponseEntity.internalServerError()
                    .body("Invalid response format from portfolio service");
            }
        }

        private String cleanErrorResponse(String body) {
            // Simple HTML stripping for error messages
            return body.replaceAll("<[^>]*>", "");
        }

    @PostMapping("/recommend/{id}")
    public CompletableFuture<ResponseEntity<RecommendationResponse>> recommendPortfolio(@PathVariable Long id) {
        log.info("Recommendation request for portfolio {}", id);
        return portfolioService.getPortfolioById(id)
            .map(portfolio -> {
                if (portfolio.getAllocation() == null || portfolio.getAllocation().isEmpty()) {
                    return CompletableFuture.completedFuture(
                        ResponseEntity.badRequest().body(new RecommendationResponse("Portfolio has no allocation data"))
                    );
                }
                
                // Async recommendation process
                return calculateRecommendation(portfolio)
                    .thenApply(recommendation -> ResponseEntity.ok(
                        new RecommendationResponse(
                            portfolio.getAllocation(),
                            recommendation,
                            "Recommendation generated successfully"
                        )
                    ))
                    .exceptionally(ex -> ResponseEntity.internalServerError()
                        .body(new RecommendationResponse("Recommendation failed: " + ex.getMessage())));
            })
            .orElse(CompletableFuture.completedFuture(
                ResponseEntity.notFound().build()
            ));
    }

    @PutMapping("/apply-recommendation/{id}")
    public ResponseEntity<RecommendationResponse> applyRecommendation(
        @PathVariable Long id,
        @Valid @RequestBody RecommendationApplicationRequest request
    ) {
        try {
            Portfolio updatedPortfolio = portfolioService.applyRecommendation(
                id, 
                request.getRecommendation(),
                request.isRecalculateDetailed()
            );
            
            return ResponseEntity.ok(
                new RecommendationResponse(
                    updatedPortfolio.getAllocation(),          
                    request.getRecommendation(),     
                    "Portfolio updated successfully",
                    updatedPortfolio.getLastCalculated()         
                    )
            );
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.notFound().build();
        } catch (InvalidAllocationException ex) {
            return ResponseEntity.badRequest()
                .body(new RecommendationResponse(ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError()
                .body(new RecommendationResponse("Update failed: " + ex.getMessage()));
        }
    }

    private CompletableFuture<Map<String, Double>> calculateRecommendation(Portfolio portfolio) {
        User user = portfolio.getUser();
        
        // Calculate sentiment score asynchronously
        CompletableFuture<Double> sentimentFuture = CompletableFuture.supplyAsync(() -> {
            List<String> allAssets = Stream.of(
                    portfolio.getStocks(),
                    portfolio.getBonds(),
                    portfolio.getCrypto()
                )
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
                
            return allAssets.stream()
                .mapToDouble(sentimentService::analyzeStockSentiment)
                .average()
                .orElse(0.5);
        });
        long start = System.currentTimeMillis();
        // Get personalized recommendation
        return sentimentFuture.thenCompose(sentimentScore -> {
            log.info("Sentiment score calculated in {} ms", System.currentTimeMillis() - start);
            long recStart = System.currentTimeMillis();
            return recommendationService.getPersonalizedRecommendation(user, portfolio, sentimentScore)
            .thenApply(rec -> {
                log.info("Recommendation generated in {} ms", System.currentTimeMillis() - recStart);
                return rec;
            });        
        });
    }

}

