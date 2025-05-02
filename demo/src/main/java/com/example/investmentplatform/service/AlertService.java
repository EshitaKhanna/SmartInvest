package com.example.investmentplatform.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.investmentplatform.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AlertService {
    private final NotificationService notificationService;
    private final WatchListService watchListService;

    public void triggerSentimentAlert(String symbol) {
        List<User> users = watchListService.getUsersWatchingSymbol(symbol);
        users.forEach(user -> {
            notificationService.sendAlert(
                user,
                "Sentiment Alert",
                String.format("Significant sentiment shift detected for %s", symbol)
            );
        });
    }
}