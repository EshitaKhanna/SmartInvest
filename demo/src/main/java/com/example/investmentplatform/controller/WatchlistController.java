package com.example.investmentplatform.controller;

import com.example.investmentplatform.entity.User;
import com.example.investmentplatform.entity.Watchlist;
import com.example.investmentplatform.entity.WatchlistId;
import com.example.investmentplatform.service.UserService;
import com.example.investmentplatform.service.WatchListService;
import com.example.investmentplatform.exceptions.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

    @Autowired
    private WatchListService watchListService;

    @Autowired
    private UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserWatchlist(@PathVariable Long userId) {
        try {
            User user = userService.getUserById(userId)
                .orElseThrow(() -> new CustomException("User not found"));

            List<Watchlist> watchlist = watchListService.getWatchlistByUser(user);
            return ResponseEntity.ok(watchlist);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to fetch watchlist: " + e.getMessage());
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addToWatchlist(@RequestBody WatchlistId watchlistId) {
        try {
            User user = userService.getUserById(watchlistId.getUserId())
                .orElseThrow(() -> new CustomException("User not found"));

            Watchlist watchlist = new Watchlist();
            watchlist.setId(watchlistId);
            watchlist.setUser(user);

            Watchlist saved = watchListService.addToWatchlist(watchlist);
            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to add to watchlist: " + e.getMessage());
        }
    }

    @DeleteMapping("/remove")
    public ResponseEntity<?> removeFromWatchlist(@RequestBody WatchlistId watchlistId) {
        try {
            watchListService.removeFromWatchlist(watchlistId);
            return ResponseEntity.ok("Removed from watchlist");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to remove: " + e.getMessage());
        }
    }
}
