package com.example.investmentplatform.service;

import com.example.investmentplatform.entity.Portfolio;
import com.example.investmentplatform.repository.PortfolioRepository;
import com.example.investmentplatform.repository.UserRepository;

import jakarta.transaction.Transactional;

import com.example.investmentplatform.entity.User;
import com.example.investmentplatform.exceptions.InvalidAllocationException;
import com.example.investmentplatform.exceptions.ResourceNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PortfolioService {
    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private UserRepository userRepository; 

    public List<Portfolio> getPortfoliosByUser(Long userId) {
        return portfolioRepository.findByUserId(userId);
    }

    @Transactional
    public Portfolio savePortfolio(Portfolio portfolio, Long userId) {
        System.out.println("Saving Portfolio for User ID: " + userId);

        // Fetch user from database
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("User not found with ID: " + userId);
        }

        // Set user object
        portfolio.setUser(userOptional.get());

        // Validate and save the portfolio
        return portfolioRepository.save(portfolio);
    }

    public Optional<Portfolio> getPortfolioById(Long id) {
        return portfolioRepository.findById(id);
    }

    @Transactional
    public void deletePortfolio(Long id) {
        portfolioRepository.deleteById(id);
    }

    @Transactional
    public void addAsset(Portfolio portfolio, String assetType, String symbol, Double weight) {
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
                throw new IllegalArgumentException("Invalid asset type");
        }

        if (weight != null) {
            Map<String, Double> allocation = portfolio.getAllocation();
            allocation.put(symbol, weight);
            portfolio.setAllocation(allocation);
        }

        portfolioRepository.save(portfolio);
    }

    @Transactional
    public void deleteAsset(Portfolio portfolio, String assetType, String symbol) {
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
                throw new IllegalArgumentException("Invalid asset type");
        }

        Map<String, Double> allocation = portfolio.getAllocation();
        allocation.remove(symbol);
        portfolio.setAllocation(allocation);

        portfolioRepository.save(portfolio);
    }

    @Transactional
    public void updateWeight(Portfolio portfolio, String symbol, Double weight) {
        Map<String, Double> allocation = portfolio.getAllocation();
        allocation.put(symbol, weight);
        portfolio.setAllocation(allocation);
        
        portfolioRepository.save(portfolio);
    }

    @Transactional
    public Portfolio applyRecommendation(Long portfolioId, Map<String, Double> recommendation, boolean recalculateDetailed){
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found"));
            
        // Validate the recommendation
        double sum = recommendation.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(sum - 1.0) > 0.0001) {
            throw new InvalidAllocationException("Allocation must sum to 1.0");
        }
        
        // Apply the recommendation
        portfolio.setAllocation(recommendation);
        
        // Recalculate detailed allocation if requested
        if (recalculateDetailed) {
            Map<String, Double> detailed = calculateDetailedAllocation(
                recommendation,
                portfolio.getStocks(),
                portfolio.getBonds(),
                portfolio.getCrypto()
            );
            portfolio.setDetailedAllocation(detailed);
        }
        
        portfolio.setLastCalculated(LocalDateTime.now());
        return portfolioRepository.save(portfolio);
    }

    private Map<String, Double> calculateDetailedAllocation(
        Map<String, Double> allocation,
        List<String> stocks,
        List<String> bonds,
        List<String> crypto) {
        
        // Validate input
        if (allocation == null) {
            throw new IllegalArgumentException("Allocation map cannot be null");
        }
        
        // Initialize result map
        Map<String, Double> detailedAllocation = new HashMap<>();
        
        // Validate and distribute bond allocation
        if (allocation.containsKey("bonds") && allocation.get("bonds") > 0) {
            if (bonds == null || bonds.isEmpty()) {
                throw new IllegalArgumentException("Bond allocation specified but no bonds provided");
            }
            double perBondAllocation = allocation.get("bonds") / bonds.size();
            bonds.forEach(bond -> detailedAllocation.put(bond, perBondAllocation));
        }
        
        // Validate and distribute stock allocation
        if (allocation.containsKey("stocks") && allocation.get("stocks") > 0) {
            if (stocks == null || stocks.isEmpty()) {
                throw new IllegalArgumentException("Stock allocation specified but no stocks provided");
            }
            double perStockAllocation = allocation.get("stocks") / stocks.size();
            stocks.forEach(stock -> detailedAllocation.put(stock, perStockAllocation));
        }
        
        // Validate and distribute crypto allocation
        if (allocation.containsKey("crypto") && allocation.get("crypto") > 0) {
            if (crypto == null || crypto.isEmpty()) {
                throw new IllegalArgumentException("Crypto allocation specified but no cryptos provided");
            }
            double perCryptoAllocation = allocation.get("crypto") / crypto.size();
            crypto.forEach(coin -> detailedAllocation.put(coin, perCryptoAllocation));
        }
        
        // Verify the sum is approximately 1.0 (accounting for floating point precision)
        double sum = detailedAllocation.values().stream()
            .mapToDouble(Double::doubleValue)
            .sum();
        
        if (Math.abs(sum - 1.0) > 0.000001) {
            throw new IllegalStateException(
                String.format("Detailed allocation sum is invalid: %.6f (should be 1.0)", sum));
        }
        
        return detailedAllocation;
    }

    

}