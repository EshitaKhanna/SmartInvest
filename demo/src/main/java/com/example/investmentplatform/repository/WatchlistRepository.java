package com.example.investmentplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.investmentplatform.entity.User;
import com.example.investmentplatform.entity.Watchlist;
import com.example.investmentplatform.entity.WatchlistId;

import java.util.List;

public interface WatchlistRepository extends JpaRepository<Watchlist, WatchlistId> {
    
    List<Watchlist> findByUser(User user);

    @Query("SELECT DISTINCT w.id.stock_symbol FROM Watchlist w")
    List<String> findAllSymbols();

    @Query("SELECT w.user FROM Watchlist w WHERE w.id.stock_symbol = :symbol")
    List<User> findUsersBySymbol(@Param("symbol") String symbol);


}