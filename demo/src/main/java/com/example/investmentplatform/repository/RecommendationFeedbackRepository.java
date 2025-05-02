package com.example.investmentplatform.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.investmentplatform.entity.RecommendationFeedback;


public interface RecommendationFeedbackRepository extends JpaRepository<RecommendationFeedback, Long> {
    
    List<RecommendationFeedback> findByPortfolioId(Long portfolioId);
    
    @Query("SELECT rf FROM RecommendationFeedback rf WHERE rf.portfolio.user.id = :userId")
    List<RecommendationFeedback> findByUserId(@Param("userId") Long userId);

    long countByTimestampAfter(LocalDateTime timestamp);

    @Query("SELECT new map(" +
       "r.portfolio.user.id as user_id, " +
       "r.portfolio.id as portfolio_id, " +
       "r.rating as rating, " +
       "r.appliedChanges as applied_changes, " +
       "r.comments as comments, " +
       "r.timestamp as timestamp) " +
       "FROM RecommendationFeedback r " +
       "WHERE r.timestamp >= :since " +
       "ORDER BY r.timestamp DESC")
    List<Map<String, Object>> findRecentFeedbackForTraining(@Param("since") LocalDateTime since);
}