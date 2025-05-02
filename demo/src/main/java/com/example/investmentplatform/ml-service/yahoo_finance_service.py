from typing import List, Tuple, Dict
import pandas as pd
import yfinance as yf
from datetime import datetime, timedelta
import logging
from flask import Flask, request, jsonify
from collections import defaultdict

logger = logging.getLogger(__name__)

class YahooFinanceService:
    
    def get_historical_prices(self, symbols: List[str], period: str = "1y") -> Dict[str, List[float]]:
        """Fetch historical closing prices for multiple symbols using yfinance."""
        if not symbols:
            return {}
        try:
            data = yf.download(
                symbols,
                period=period,
                group_by='ticker',
                progress=False,
                auto_adjust=True
            )
            prices = {}
            for symbol in symbols:
                if symbol in data:
                    closes = data[symbol]['Close'].dropna()
                    prices[symbol] = closes.tolist()
                else:
                    prices[symbol] = []
                    logger.warning(f"No data for {symbol}")
            return prices
        except Exception as e:
            logger.error(f"Download failed: {str(e)}")
            return {s: [] for s in symbols}

    def get_historical_returns(self, symbols: List[Tuple[str, str]], period: str) -> Dict[str, List[float]]:
        """Calculate historical returns for symbols, handling different asset types uniformly."""
        symbol_list = [s[0] for s in symbols]
        prices = self.get_historical_prices(symbol_list, period)
        returns = {}
        for symbol, _ in symbols:
            prices_list = prices.get(symbol, [])
            returns[symbol] = self._calculate_returns(prices_list)
            if not returns[symbol]:
                logger.warning(f"No returns for {symbol}")
        return returns

    def _calculate_returns(self, prices: List[float]) -> List[float]:
        """Compute daily percentage returns from price data."""
        if len(prices) < 2:
            return []
        returns = []
        for i in range(1, len(prices)):
            prev = prices[i-1]
            curr = prices[i]
            if prev == 0:
                returns.append(0.0)
            else:
                returns.append((curr - prev) / prev)
        return returns


    def get_total_portfolio_value_from_allocations(self, portfolios: List[Dict], base_amount: float) -> float:
        """Compute total USD value of multiple portfolios using detailed allocations and base investment amount."""

        if base_amount <= 0:
            logger.error("Invalid base amount: must be greater than 0.")
            return 0.0
    
        total_allocations = defaultdict(float)
        for portfolio in portfolios:
            allocations = portfolio.get("detailedAllocation", {})
            for symbol, weight in allocations.items():
                total_allocations[symbol] += weight * base_amount

        if not total_allocations:
            return 0.0

        # Get latest price for each symbol
        try:
            prices_df = yf.download(
                list(total_allocations.keys()),
                period="1d",
                progress=False,
                auto_adjust=True
            )["Close"]

            if isinstance(prices_df, pd.Series):
                prices_df = prices_df.to_frame()

            latest_prices = prices_df.iloc[-1].to_dict()

            # Validate prices to ensure no NaN values are involved
            for symbol, price in latest_prices.items():
                if pd.isna(price):
                    logger.error(f"Invalid price for symbol {symbol}: NaN value encountered")
                    latest_prices[symbol] = 0.0  # Set to 0.0 or handle as needed

            total_value = sum(latest_prices.get(symbol, 0.0) * quantity for symbol, quantity in total_allocations.items())
            
            if pd.isna(total_value):
                logger.error("Invalid calculation result: NaN value in total portfolio value")
                return 0.0
        
            return total_value

        except Exception as e:
            logger.error(f"Error calculating portfolio value: {str(e)}")
            return 0.0
        

