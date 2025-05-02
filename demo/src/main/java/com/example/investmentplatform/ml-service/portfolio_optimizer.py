from typing import Dict, List, Tuple
import numpy as np
import scipy
import logging
from yahoo_finance_service import YahooFinanceService 

logger = logging.getLogger(__name__)


class PortfolioOptimizer:
    """
    OPTIMIZATION PARAMETERS:
        - Data Period: 2 years historical returns
        - Risk Tolerance: 0-1 scale (0.5 = MODERATE)
    - Sentiment Impact: 
        • Stocks: +50% of sentiment score 
        • Bonds: -30% of sentiment score (inverse)
        • Crypto: +80% of sentiment score
    - Minimum Allocation: 1% per asset
    - Constraints by Goal:
        • INCOME: Bonds 40-70%, Stocks 20-50%, Crypto ≤10%
        • GROWTH: Stocks+Crypto ≥60%, Bonds ≤20%
        • BALANCED: Stocks 40-60%, Bonds 20-40%, Crypto ≤20%
  """
    def __init__(self):
        self.yahoo_finance_service = YahooFinanceService()
        self.default_period = "2y"

    def _align_returns(self, returns: Dict[str, List[float]]) -> np.ndarray:
        """Align return sequences to the same length with front-padding, excluding symbols with no data."""
        valid_returns = {k: v for k, v in returns.items() if len(v) > 0}
        if not valid_returns:
            return np.array([])
        
        max_length = max(len(v) for v in valid_returns.values())
        aligned = []
        for symbol in valid_returns:
            original = valid_returns[symbol]
            if len(original) < max_length:
                padded = [0.0] * (max_length - len(original)) + original
            else:
                padded = original[-max_length:]
            aligned.append(padded)
        return np.array(aligned).T

    
    def calculate_expected_returns(self, symbols: List[Tuple[str, str]]) -> Dict[str, float]:
        """Calculate expected returns for valid symbols with historical data."""

        historical_returns = self.yahoo_finance_service.get_historical_returns(symbols)
        valid_returns = {k: v for k, v in historical_returns.items() if len(v) > 0}

        return {symbol: np.mean(returns) for symbol, returns in valid_returns.items()}

    
    def calculate_portfolio_risk(self, symbols: List[Tuple[str, str]], weights: List[float] = None) -> float:
        """Calculate portfolio risk (std dev) considering only valid symbols."""
        historical_returns = self.yahoo_finance_service.get_historical_returns(symbols)
        valid_symbols = [s[0] for s in symbols if len(historical_returns.get(s[0], [])) > 0]
        if not valid_symbols:
            return 0.0
        
        valid_returns = {s: historical_returns[s] for s in valid_symbols}
        aligned_returns = self._align_returns(valid_returns)
        if aligned_returns.size == 0:
            return 0.0
        
        covariance_matrix = np.cov(aligned_returns, rowvar=False)
        if weights is None:
            weights = np.ones(len(valid_symbols)) / len(valid_symbols)
        else:
            weights = np.array(weights)
        
        portfolio_variance = np.dot(weights.T, np.dot(covariance_matrix, weights))
        portfolio_risk = np.sqrt(portfolio_variance)
        return portfolio_risk


    def optimize_portfolio(
        self, 
        symbols: List[Tuple[str, str]], 
        risk_tolerance: float, 
        sentiment_scores: Dict[str, float],
        investment_goal: str) -> Dict[str, float]:
        """ 
        Optimizes portfolio weights using Mean-Variance Optimization with:
        - Sentiment integration for multiple asset classes
        - Investment goal constraints
        - Risk-adjusted returns
        - Minimum 1% allocation per asset
        - Balanced constraints based on investment goals
        
        Returns:
            Dict[str, float]: Optimized weights for each symbol
            or {"error": message} if optimization fails
        """
        
        logger.info(f"Starting optimization for {len(symbols)} assets")
        
        # 1. Historical Returns Fetching with Validation
        try:
            historical_returns = self.yahoo_finance_service.get_historical_returns(
                symbols, 
                period=self.default_period
            )
            
            # Filter out symbols without sufficient data
            valid_symbols = [s[0] for s in symbols if len(historical_returns.get(s[0], [])) > 0]
            if not valid_symbols:
                logger.error("No symbols with valid historical data")
                return {"error": "No valid historical data"}
            
            # Get filtered symbols with asset type information
            filtered_symbols = [s for s in symbols if s[0] in valid_symbols]
            filtered_returns = {s[0]: historical_returns[s[0]] for s in filtered_symbols}
            
            # 2. Data Alignment and Validation
            aligned_returns = self._align_returns(filtered_returns)
            if aligned_returns.size == 0:
                logger.error("No valid data after alignment")
                return {"error": "Insufficient data"}
            
            n_obs, n_assets = aligned_returns.shape
            if n_obs < 2 or n_assets < 1:
                logger.error("Insufficient data points")
                return {"error": "Insufficient data"}
            
            # 3. Sentiment-Adjusted Expected Returns Calculation
            expected_returns = {}
            asset_type_weights = {"stocks": 1.0, "bonds": 0.8, "crypto": 1.2}  # Base multipliers
            
            for symbol, asset_type in filtered_symbols:
                base_return = np.mean(filtered_returns[symbol])
                sentiment = sentiment_scores.get(symbol, 0.5)  # Default to neutral
                
                # More balanced sentiment adjustments
                if asset_type == "stocks":
                    adj_return = base_return * (1 + 0.5*sentiment) * asset_type_weights["stocks"]
                elif asset_type == "bonds":
                    adj_return = base_return * (1 + 0.3*(1-sentiment)) * asset_type_weights["bonds"]
                elif asset_type == "crypto":
                    adj_return = base_return * (1 + 0.8*sentiment) * asset_type_weights["crypto"]
                else:
                    adj_return = base_return * 1.0
                    
                expected_returns[symbol] = adj_return

            # 4. Investment Goal Constraints
            constraints = [{'type': 'eq', 'fun': lambda w: np.sum(w) - 1}]
            
            # Get asset type indices
            stock_indices = [i for i, (_, at) in enumerate(filtered_symbols) if at == "stocks"]
            bond_indices = [i for i, (_, at) in enumerate(filtered_symbols) if at == "bonds"]
            crypto_indices = [i for i, (_, at) in enumerate(filtered_symbols) if at == "crypto"]
            
            # Minimum 1% allocation per asset
            for i in range(n_assets):
                constraints.append({
                    'type': 'ineq',
                    'fun': lambda w, i=i: w[i] - 0.01
                })

            # Goal-specific constraints
            if investment_goal.lower() == "growth":
                constraints.extend([
                    # Growth assets (stocks + crypto) between 60-90%
                    {'type': 'ineq', 'fun': lambda w: sum(w[i] for i in stock_indices + crypto_indices) - 0.6},
                    {'type': 'ineq', 'fun': lambda w: 0.9 - sum(w[i] for i in stock_indices + crypto_indices)},
                    # Bonds between 5-20%
                    {'type': 'ineq', 'fun': lambda w: sum(w[i] for i in bond_indices) - 0.05},
                    {'type': 'ineq', 'fun': lambda w: 0.2 - sum(w[i] for i in bond_indices)}
                ])
            elif investment_goal.lower() == "income":
                constraints.extend([
                    # Bonds between 40-70%
                    {'type': 'ineq', 'fun': lambda w: sum(w[i] for i in bond_indices) - 0.4},
                    {'type': 'ineq', 'fun': lambda w: 0.7 - sum(w[i] for i in bond_indices)},
                    # Stocks between 20-50%
                    {'type': 'ineq', 'fun': lambda w: sum(w[i] for i in stock_indices) - 0.2},
                    {'type': 'ineq', 'fun': lambda w: 0.5 - sum(w[i] for i in stock_indices)},
                    # Crypto max 10%
                    {'type': 'ineq', 'fun': lambda w: 0.1 - sum(w[i] for i in crypto_indices)}
                ])
            elif investment_goal.lower() == "balanced":
                constraints.extend([
                    # Stocks 40-60%
                    {'type': 'ineq', 'fun': lambda w: sum(w[i] for i in stock_indices) - 0.4},
                    {'type': 'ineq', 'fun': lambda w: 0.6 - sum(w[i] for i in stock_indices)},
                    # Bonds 20-40%
                    {'type': 'ineq', 'fun': lambda w: sum(w[i] for i in bond_indices) - 0.2},
                    {'type': 'ineq', 'fun': lambda w: 0.4 - sum(w[i] for i in bond_indices)},
                    # Crypto max 20%
                    {'type': 'ineq', 'fun': lambda w: 0.2 - sum(w[i] for i in crypto_indices)}
                ])

            # 5. Optimization Setup
            covariance_matrix = np.cov(aligned_returns, rowvar=False)
            
            # Objective function: Balance risk vs return based on risk tolerance
            def objective(weights):
                port_variance = weights.T @ covariance_matrix @ weights
                port_return = np.dot(weights, list(expected_returns.values()))
                return np.sqrt(port_variance) - risk_tolerance * port_return
            
            bounds = [(0.01, 1) for _ in range(n_assets)]  # Minimum 1% allocation
            initial_guess = np.ones(n_assets) / n_assets  # Equal weight starting point
            
            # 6. Run Optimization
            result = scipy.optimize.minimize(
                objective,
                initial_guess,
                method='SLSQP',
                bounds=bounds,
                constraints=constraints,
                options={'maxiter': 1000}
            )
            
            # 7. Post-processing
            if not result.success:
                logger.error(f"Optimization failed: {result.message}")
                return {"error": "Optimization failed"}
            
            # Apply minimum allocation and normalize
            optimized_weights = {
                symbol[0]: max(round(weight, 4), 0.01)  # Enforce 1% minimum
                for symbol, weight in zip(filtered_symbols, result.x)
            }
            
            # Normalize to ensure sum = 1
            total = sum(optimized_weights.values())
            if total == 0:
                return {"error": "All weights filtered out"}
                
            normalized_weights = {k: v/total for k, v in optimized_weights.items()}
            
            logger.info(f"Optimization successful. Weights: {normalized_weights}")
            return normalized_weights
        
        except Exception as e:
            logger.exception("Optimization error")
            return {"error": str(e)}
    
    # def optimize_portfolio(
    #     self, 
    #     symbols: List[Tuple[str, str]], 
    #     risk_tolerance: float, 
    #     sentiment_scores: Dict[str, float],
    #     investment_goal: str) -> Dict[str, float]:

    #     """ Optimizes portfolio weights using Mean-Variance Optimization with:
    #     - Sentiment integration for multiple asset classes
    #     - Investment goal constraints
    #     - Risk-adjusted returns
    #     """

    #     logger.info(f"Starting optimization for {len(symbols)} assets")
        
    #     # 1. Historical Returns Fetching with Validation
    #     try:
    #         historical_returns = self.yahoo_finance_service.get_historical_returns(
    #             symbols, 
    #             period=self.default_period
    #         )
    #         # Filter out symbols without sufficient data
    #         valid_symbols = [s[0] for s in symbols if len(historical_returns.get(s[0], [])) > 0]
    #         if not valid_symbols:
    #             logger.error("No symbols with valid historical data")
    #             return {"error": "No valid historical data"}
            
    #         # Get filtered symbols with asset type information
    #         filtered_symbols = [s for s in symbols if s[0] in valid_symbols]
    #         filtered_returns = {s[0]: historical_returns[s[0]] for s in filtered_symbols}
            
    #         # 2. Data Alignment and Validation
    #         aligned_returns = self._align_returns(filtered_returns)
    #         if aligned_returns.size == 0:
    #             logger.error("No valid data after alignment")
    #             return {"error": "Insufficient data"}
            
    #         n_obs, n_assets = aligned_returns.shape
    #         if n_obs < 2 or n_assets < 1:
    #             logger.error("Insufficient data points")
    #             return {"error": "Insufficient data"}
            
    #         # 3. Sentiment-Adjusted Expected Returns Calculation
    #         expected_returns = {}
    #         asset_type_weights = {"stocks": 1.0, "bonds": 0.8, "crypto": 1.2}  # Base multipliers
    #         for symbol, asset_type in filtered_symbols:
    #             base_return = np.mean(filtered_returns[symbol])
    #             sentiment = sentiment_scores.get(symbol, 0.5)  # Default to neutral
                
    #             # Asset-specific sentiment adjustments
    #             if asset_type == "stocks":
    #                 # Stocks: Positive sentiment amplifies expected returns
    #                 adj_return = base_return * (1 + sentiment)  * asset_type_weights["stocks"]
    #             elif asset_type == "bonds":
    #                 # Bonds: Positive sentiment reduces volatility assumption
    #                 adj_return = base_return * (1 + (1 - sentiment)) * asset_type_weights["bonds"]  # Inverse relationship
    #             elif asset_type == "crypto":
    #                 # Crypto: Higher sentiment impact with volatility scaling
    #                 adj_return = base_return * (1 + sentiment * 1.5) * asset_type_weights["crypto"]
    #             else:
    #                 # Unknown asset type, use neutral adjustment
    #                 adj_return = base_return * 1.0
                    
    #             expected_returns[symbol] = adj_return


    #         # 4. Investment Goal Constraints
    #         constraints = [{'type': 'eq', 'fun': lambda w: np.sum(w) - 1}]

    #         # Get asset type indices
    #         stock_indices = [i for i, (_, at) in enumerate(filtered_symbols) if at == "stocks"]
    #         bond_indices = [i for i, (_, at) in enumerate(filtered_symbols) if at == "bonds"]
    #         crypto_indices = [i for i, (_, at) in enumerate(filtered_symbols) if at == "crypto"]

    
    #         if investment_goal.lower() == "growth":
    #             constraints = [
    #                 {'type': 'eq', 'fun': lambda w: np.sum(w) - 1},
    #                 # Minimum 60% growth assets (stocks + crypto)
    #                 {'type': 'ineq', 'fun': lambda w: np.sum(w[[i for i, (s, at) in enumerate(symbols) if at in ["stocks", "crypto"]]]) - 0.6},
    #                 # Maximum 20% bonds
    #                 {'type': 'ineq', 'fun': lambda w: 0.2 - np.sum(w[[i for i, (s, at) in enumerate(symbols) if at == "bonds"]])}
    #             ]
    #         elif investment_goal.lower() == "income": # Income: Emphasize bonds
    #             # Minimum 40% bonds
    #             if bond_indices:
    #                 constraints.append({
    #                     'type': 'ineq',
    #                     'fun': lambda w: sum(w[i] for i in bond_indices) - 0.4
    #                 })
                    
    #             # Maximum 30% crypto
    #             if crypto_indices:
    #                 constraints.append({
    #                     'type': 'ineq',
    #                     'fun': lambda w: 0.3 - sum(w[i] for i in crypto_indices)
    #                 })

    #         elif investment_goal.lower() == "balanced": # Balanced: Range-based constraints
    #             # 40-60% stocks, 20-40% bonds, 0-20% crypto
    #             if stock_indices:
    #                 constraints.extend([
    #                     {'type': 'ineq', 'fun': lambda w: sum(w[i] for i in stock_indices) - 0.4},
    #                     {'type': 'ineq', 'fun': lambda w: 0.6 - sum(w[i] for i in stock_indices)}
    #                 ])
    #             if bond_indices:
    #                 constraints.extend([
    #                     {'type': 'ineq', 'fun': lambda w: sum(w[i] for i in bond_indices) - 0.2},
    #                     {'type': 'ineq', 'fun': lambda w: 0.4 - sum(w[i] for i in bond_indices)}
    #                 ])
    #             if crypto_indices:
    #                 constraints.append({
    #                     'type': 'ineq', 
    #                     'fun': lambda w: 0.2 - sum(w[i] for i in crypto_indices)
    #                 })

        

    #         # 5. Optimization Setup
    #         covariance_matrix = np.cov(aligned_returns, rowvar=False)
            
    #         # Objective function: Balance risk vs return based on risk tolerance
    #         def objective(weights):
    #             port_variance = weights.T @ covariance_matrix @ weights
    #             port_return = np.dot(weights, list(expected_returns.values()))
    #             return np.sqrt(port_variance) - risk_tolerance * port_return
            
    #         constraints = ({'type': 'eq', 'fun': lambda w: np.sum(w) - 1})
    #         bounds = [(0, 1) for _ in range(n_assets)]
    #         initial_guess = np.ones(n_assets) / n_assets # Equal weight starting point
            
    #         # 6. Run Optimization
    #         result = scipy.optimize.minimize(
    #             objective,
    #             initial_guess,
    #             method='SLSQP',
    #             bounds=bounds,
    #             constraints=constraints
    #         )
            
    #         # 7. Post-processing
    #         if not result.success:
    #             logger.error(f"Optimization failed: {result.message}")
    #             return {"error": "Optimization failed"}
            
    #         # Generate final weights with filtering for negligible allocations
    #         optimized_weights = {
    #             symbol[0]: round(weight, 4)
    #             for symbol, weight in zip(filtered_symbols, result.x)
    #             if round(weight, 4) >= 0.0001 # Filter out tiny allocations
    #         }

    #         # Normalize weights after filtering
    #         total = sum(optimized_weights.values())
    #         if total == 0:
    #             return {"error": "All weights filtered out"}
                
    #         return {k: v/total for k, v in optimized_weights.items()}
        
    #     except Exception as e:
    #         logger.exception("Optimization error")
    #         return {"error": str(e)}

