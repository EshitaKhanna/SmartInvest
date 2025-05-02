package com.example.investmentplatform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.vladmihalcea.hibernate.type.json.JsonType;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "portfolio")

@Data
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER) 
    @JoinColumn(name = "user_id", nullable = false)
    private User user;   // Foreign key to the User entity

    @ElementCollection
    @CollectionTable(name = "portfolio_stocks", joinColumns = @JoinColumn(name = "portfolio_id"))
    @Column(name = "stock_symbol")
    private List<String> stocks;  // List of stock symbols (e.g., AAPL, GOOGL)

    @ElementCollection
    @CollectionTable(name = "portfolio_bonds", joinColumns = @JoinColumn(name = "portfolio_id"))
    @Column(name = "bond_symbol")
    private List<String> bonds;  // List of bond symbols

    @ElementCollection
    @CollectionTable(name = "portfolio_crypto", joinColumns = @JoinColumn(name = "portfolio_id"))
    @Column(name = "crypto_symbol")
    private List<String> crypto;  // List of cryptocurrency symbols

    @Type(JsonType.class)
    @Column(name = "allocation", nullable = false, columnDefinition = "jsonb")
    private Map<String, Double> allocation;   // JSON string representing asset allocation (e.g., {"stocks": 60, "bonds": 30, "crypto": 10})
    
    @Type(JsonType.class)
    @Column(name = "detailed_allocation", columnDefinition = "jsonb")
    private Map<String, Double> detailedAllocation;  // Format: {"AAPL": 0.45, "MSFT": 0.55}

     @Column(name = "performance_score")
    private Double performanceScore;

    @Column(name = "risk_adjusted_return")
    private Double riskAdjustedReturn;

    @Column(name = "last_calculated")
    private LocalDateTime lastCalculated;
}