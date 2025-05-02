import math
from flask import Flask, request, jsonify
import joblib
import requests
from portfolio_optimizer import PortfolioOptimizer
from sentiment_pipeline import SentimentPipeline
from recommendation_engine import RecommendationEngine
from yahoo_finance_service import YahooFinanceService
from flask_cors import CORS
import logging
import pandas as pd
from flask_jwt_extended import jwt_required, get_jwt_identity

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)
app = Flask(__name__)
CORS(app, resources={r"/recommend": {"origins": "*"}})
service = YahooFinanceService()

# Configuration
NEWSAPI_KEY = "5bf4203217d340019440f212578d1dbc"
DB_NAME = "investment_platform"
DB_USER = "Eshita"

pipeline = SentimentPipeline(NEWSAPI_KEY, DB_NAME, DB_USER)
optimizer = PortfolioOptimizer()
recommender = RecommendationEngine()

SPRING_BOOT_API_URL = "http://localhost:8080/api"


def classify_asset(symbol: str) -> str:
    """Classify assets based on symbol conventions"""
    if "-USD" in symbol:
        return "crypto"
    elif "^" in symbol:
        return "bond"
    return "stock"

@app.route("/analyze", methods=["GET", "POST"])
def analyze():
    if request.method == "GET":
        return jsonify({"message": "Use a POST request with {'assets': {'stocks': ['', '']}}"}), 200
    
    try:
        data = request.get_json()
        if not data or "assets" not in data:
            return jsonify({"error": "Missing 'assets' in request body"}), 400

        assets = data.get("assets", {})
        results = {"stocks": {}, "bonds": {}, "crypto": {}}

        # Validate and process assets
        for asset_type in ["stocks", "bonds", "crypto"]:
            for symbol in assets.get(asset_type, []):
                try:
                    analysis_result = pipeline.analyze_asset(symbol, asset_type)
                    results[asset_type][symbol] = analysis_result
                except Exception as e:
                    logger.error(f"Analysis failed for {symbol}: {str(e)}")
                    results[asset_type][symbol] = {"error": str(e)}

        return jsonify(results), 200

    except Exception as e:
        logger.error(f"Analysis error: {str(e)}")
        return jsonify({"error": str(e)}), 500
    
@app.route("/optimize", methods=["GET","POST"])
def optimize():
    if request.method == "GET":
        return jsonify({"message": "Use a POST request with {'assets': {'stocks': ['']}, 'risk_tolerance': '', 'investment_goal': ''}"}), 200
    try:
        logger.info(f"Received POST request for optimization:");
        data = request.get_json()

        required_fields = ["assets", "risk_tolerance", "investment_goal", "sentiment_scores"]
        if not all(field in data for field in required_fields):
            return jsonify({"error": "Missing required fields"}), 400

        assets = data["assets"]
        risk_tolerance = data.get("risk_tolerance")
        investment_goal = data.get("investment_goal")
        sentiment_scores = data.get("sentiment_scores")

        logger.info(f"Received assets: {assets}")
        logger.info(f"Received risk_tolerance: {risk_tolerance}")
        logger.info(f"Received investment goal: {investment_goal}")
        logger.info(f"Received sentiment scores: {sentiment_scores}")

        # Validate inputs
        if not 0 <= risk_tolerance <= 1:
            return jsonify({"error": "Risk tolerance must be between 0 and 1"}), 400

        # Validate asset types
        asset_types = {"stocks", "bonds", "crypto"}
        if not all(at in asset_types for at in assets.keys()):
            return jsonify({"error": "Invalid asset types"}), 400

        # Prepare symbols with asset types
        symbols = []
        for asset_type, sym_list in assets.items():
            symbols.extend([(sym, asset_type) for sym in sym_list])

        logger.info(f"Prepared symbols: {symbols}")

        # Run optimization with multi-asset support
        optimized_weights = optimizer.optimize_portfolio(
            symbols=symbols,
            risk_tolerance=risk_tolerance,
            investment_goal=investment_goal,
            sentiment_scores=sentiment_scores
        )

        return jsonify({
            "optimized_weights": optimized_weights,
            "risk_profile": risk_tolerance,
            "investment_goal": investment_goal
        }), 200

    except Exception as e:
        logger.error(f"Optimization failed: {str(e)}")
        return jsonify({
            "error": "Optimization failed",
            "details": str(e)
        }), 500

@app.route("/recommend", methods=["POST", "OPTIONS"])
def recommend():
    if request.method == "OPTIONS":
        return _build_cors_preflight_response()

    if request.method == "GET":
        return jsonify({"message": "Use a POST request with {'risk_tolerance': '', 'investment_goal': '', 'time_horizon': ''}"}), 200
    
    try:
        if not request.is_json:
            return jsonify({"error": "Request must be JSON"}), 400 
        
        data = request.get_json()
        print("\n--- Incoming JSON Data ---")
        print(data)
        
        # Required fields with income
        required_fields = [
            "age", "annual_income", "risk_tolerance", "investment_goal",
            "stocks_current", "bonds_current", "crypto_current", "sentiment_score"
        ]
        
        # Validate presence of all fields
        missing = [field for field in required_fields if field not in data]
        if missing:
            return jsonify({"error": f"Missing fields: {missing}"}), 400

        # Validate numerical ranges
        allocation_fields = ["stocks_current", "bonds_current", "crypto_current"]
        for field in allocation_fields:
            value = data.get(field, 0)
            if not 0 <= value <= 1:
                return jsonify({"error": f"{field} must be between 0 and 1"}), 400

        if not 0 <= data.get("sentiment_score", 0) <= 1:
            return jsonify({"error": "sentiment_score must be between 0 and 1"}), 400
        
        # Generate recommendation using the RecommendationEngine
        result = recommender.generate_recommendation(data)
        
        # Validate output
        total = sum(result.values())
        if total != 1.0:  # Allow small floating point differences
            return jsonify({"error": f"Invalid recommendation sum: {total}"}), 400
            
        return jsonify(result)
       
    except ValueError as e:
        logger.error(f"Validation error: {str(e)}")
        return jsonify({"error": str(e)}), 400
    
    except Exception as e:
        logger.error(f"Recommendation error: {str(e)}")
        return jsonify({"error": "Internal server error"}), 500
    
@app.route("/sentiment/<symbol>", methods=["GET","POST"])
def analyze_sentiment(symbol):
    
    try:
        score = pipeline.run_and_return(symbol)
        return jsonify({"symbol": symbol, "sentiment_score": round(score, 4),  "status": "success"})
    except Exception as e:
        logger.error(f"Sentiment fetch failed: {e}")
        return jsonify({"error": str(e), "fallback": 0.5, "status": "error"}),500

@app.route('/api/market-return', methods=['GET'])
def get_market_return():
    symbol = request.args.get('symbol', '^GSPC')
    try:
        market_return = service.get_market_return(symbol)
        return jsonify({"return": market_return})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/performance', methods=['GET'])
def get_portfolio_metrics():
    # This is an example endpoint; you can wire it to read actual portfolio assets
    try:
        # Simulated data for portfolio return and volatility
        # Replace this with logic to calculate based on assets in portfolio
        return jsonify({
            "portfolioReturn": 0.12,
            "portfolioVolatility": 0.07
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/portfolio/total-value', methods=['POST'])
def get_portfolio_value():
    try:
        data = request.get_json()
        
        if not data or "portfolios" not in data or "base_amount" not in data:
            return jsonify({
                "error": "Invalid input: missing required fields",
                "status": "error"
            }), 400

        portfolios = data["portfolios"]
        base_amount = float(data["base_amount"])  # Ensure numeric type

        logger.debug(f"Portfolios: {portfolios}, Base Amount: {base_amount}")

        total_value = service.get_total_portfolio_value_from_allocations(portfolios, base_amount)

        if math.isnan(total_value):
            raise ValueError("Invalid calculation result (NaN)")
        
        return jsonify({
            "value": float(total_value),
            "status": "success"
        }), 200
        
    except Exception as e:
        logger.error(f"Error processing request: {str(e)}")
        return jsonify({
            "error": "Processing error",
            "details": str(e),
            "status": "error"
        }), 500
    
# Add error handler for all exceptions
@app.errorhandler(Exception)
def handle_exception(e):
    logger.error(f"Unhandled exception: {str(e)}")
    return jsonify({
        "error": "Internal server error",
        "details": str(e),
        "status": "error"
    }), 500

def _build_cors_preflight_response():
    response = jsonify({"status": "preflight"})
    response.headers.add("Access-Control-Allow-Origin", "*")
    response.headers.add("Access-Control-Allow-Headers", "*")
    response.headers.add("Access-Control-Allow-Methods", "*")
    return response

@app.after_request
def log_response(response):
    app.logger.debug(f"Response headers: {dict(response.headers)}")
    app.logger.debug(f"Response status: {response.status}")
    return response

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5001, debug=True)