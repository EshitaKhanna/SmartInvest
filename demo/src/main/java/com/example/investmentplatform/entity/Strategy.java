package com.example.investmentplatform.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "strategy")
@Data
public class Strategy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "risk_tolerance", nullable = false)
    private String riskTolerance;

    @Column(nullable = false, columnDefinition = "jsonb")
    private String allocation; // JSON string
}