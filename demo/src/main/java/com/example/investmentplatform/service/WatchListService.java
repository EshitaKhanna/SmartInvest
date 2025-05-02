package com.example.investmentplatform.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.investmentplatform.entity.User;
import com.example.investmentplatform.entity.Watchlist;
import com.example.investmentplatform.entity.WatchlistId;
import com.example.investmentplatform.repository.WatchlistRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WatchListService {
    @Autowired
    private WatchlistRepository watchlistRepository;

    public List<Watchlist> getWatchlistByUser(User user) {
        return watchlistRepository.findByUser(user);
    }

    public Watchlist addToWatchlist(Watchlist watchlist) {
        return watchlistRepository.save(watchlist);
    }

    public void removeFromWatchlist(WatchlistId watchlistId) {
        watchlistRepository.deleteById(watchlistId);
    }

    public boolean isInWatchlist(WatchlistId id) {
        return watchlistRepository.existsById(id);
    }

    public List<String> getAllWatchlistSymbols() {
        return watchlistRepository.findAllSymbols();
    }
    public List<User> getUsersWatchingSymbol(String symbol) {
        return watchlistRepository.findUsersBySymbol(symbol);
    }
    
}
