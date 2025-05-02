package com.example.investmentplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.investmentplatform.entity.SentimentHistory;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SentimentHistoryRepository extends JpaRepository<SentimentHistory, Long> {
    List<SentimentHistory> findByStockSymbol(String stockSymbol);

    List<SentimentHistory> findByStockSymbolOrderByTimestampDesc(String stockSymbol);

    List<SentimentHistory> findTop10ByStockSymbolOrderByTimestampDesc(String stockSymbol);

    @Query("SELECT sh FROM SentimentHistory sh WHERE sh.stockSymbol = :symbol ORDER BY sh.timestamp DESC LIMIT 10")
    List<SentimentHistory> findRecentBySymbol(@Param("symbol") String symbol, @Param("cutoff") LocalDateTime cutoff);


}