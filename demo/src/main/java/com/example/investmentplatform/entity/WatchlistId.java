package com.example.investmentplatform.entity;

import lombok.Data;

import java.io.Serializable;

import jakarta.persistence.Embeddable;

@Embeddable
@Data
public class WatchlistId implements Serializable {
    private Long userId;
    private String stock_symbol;
}
