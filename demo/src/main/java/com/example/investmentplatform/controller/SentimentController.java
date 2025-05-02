package com.example.investmentplatform.controller;

import com.example.investmentplatform.entity.SentimentHistory;
import com.example.investmentplatform.service.SentimentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sentiment")
public class SentimentController {
    @Autowired
    private SentimentService sentimentService;

    @GetMapping("/{stockSymbol}")
    public List<SentimentHistory> getSentimentData(@PathVariable String stockSymbol) {
        return sentimentService.getSentimentData(stockSymbol);
    }
}