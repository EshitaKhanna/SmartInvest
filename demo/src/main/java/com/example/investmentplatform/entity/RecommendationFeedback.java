package com.example.investmentplatform.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recommendation_feedback")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(nullable = false)
    private Integer rating; // 1-5 scale

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "applied_changes")
    private Boolean appliedChanges;

    @PrePersist
    public void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}
