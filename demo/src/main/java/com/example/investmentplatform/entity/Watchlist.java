package com.example.investmentplatform.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "watchlist")
@Data
public class Watchlist {

    @EmbeddedId
    private WatchlistId id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

}