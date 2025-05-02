package com.example.investmentplatform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sentiment_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SentimentHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_symbol", nullable = false)
    private String stockSymbol;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name="title", nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(name="content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "sentiment_score", nullable = false)
    private Double sentimentScore;

    @Column(name = "moving_avg")
    private Double movingAvg;

    @Column(name = "std_dev")
    private Double stdDev;

}