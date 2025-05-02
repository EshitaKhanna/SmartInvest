package com.example.investmentplatform.repository;

import com.example.investmentplatform.entity.Portfolio;
import com.example.investmentplatform.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class PortfolioRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Test
    void findByUserId_Success() {
        User user = new User();
        user.setEmail("test@example.com");
        entityManager.persist(user);

        Portfolio portfolio = new Portfolio();
        portfolio.setUser(user);
        entityManager.persist(portfolio);
        entityManager.flush();

        assertFalse(portfolioRepository.findByUserId(user.getId()).isEmpty());
    }
}