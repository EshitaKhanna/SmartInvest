# SmartInvest: Personalized Portfolio Platform

SmartInvest is a full-stack investment platform that helps users build, manage, and optimize personalized portfolios using a combination of  financial sentiment analysis, machine learning, and user profiling.

It features:

- **JWT-based Authentication**
- **Portfolio Creation & Management**
- **Risk-Tolerance Aware Portfolio Optimization**
- **Sentiment Analysis from Financial News**
- **ML-based Personalized Recommendations**
- **Java (Spring Boot)** backend + **Python (Flask)** ML microservice

---

## Key Features

- **User Registration & Authentication**: JWT-secured login and registration with investment profiling

- **Portfolio Management**: Create, update, and analyze portfolios with stocks, bonds, and cryptocurrency

- **Portfolio Optimization**: Dynamic rebalancing using user preferences (risk tolerance, investment goals) and market sentiment

- **ML-Powered Recommendations**: Real-time, personalized asset allocation using a retrainable RandomForest model

- **Sentiment Analysis**: Extract sentiment scores for assets using FinBERT on news from NewsAPI, Google News, and Yahoo

- **Watchlist**: Add assets to a personal watchlist 

---

## Tech Stack

| Layer             | Technology                                  |
|------------------|---------------------------------------------|
| Backend API      | Java, Spring Boot, Spring Security, JPA  |
| ML Services      | Python (Flask), FinBERT, scikit-learn,  yfinance, pandas, joblib, transformers     |
| Database         | PostgreSQL                                  |
| Communication    | REST APIs   |
| Security         | JWT                          |
|News Source APIs	 | NewsAPI, Yahoo Finance     |

---
