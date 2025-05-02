package com.example.investmentplatform.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.investmentplatform.entity.Strategy;

@Repository
public interface StrategyRepository extends JpaRepository<Strategy, Long> {
    List<Strategy> findByRiskTolerance(String riskTolerance);
}
