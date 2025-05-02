# recommendation_engine.py
import joblib
import pandas as pd

class RecommendationEngine:
    def __init__(self):
        self.model = joblib.load("recommendation_model.pkl")
        self.income_mapping = {
            "<50": 0,
            "50-100": 1,
            "100-200": 2,
            ">200": 3
        }

    def generate_recommendation(self, features):
        required_keys = ["age", "annual_income", "risk_tolerance", "investment_goal", 
                    "stocks_current", "bonds_current", "crypto_current", "sentiment_score"]
        
        if not all(key in features for key in required_keys):
            raise ValueError("Missing required features")

         # Validate income bracket
        if features["annual_income"] not in self.income_mapping:
            raise ValueError(f"Invalid income bracket: {features['annual_income']}. Valid values: {list(self.income_mapping.keys())}")
        
        # Ensure values are within expected ranges
        if not (0 <= features["stocks_current"] <= 1) or \
        not (0 <= features["bonds_current"] <= 1) or \
        not (0 <= features["crypto_current"] <= 1):
            raise ValueError("Allocation values must be between 0 and 1")
    
        
        mapping = {
            "conservative": 0,
            "moderate": 1,
            "aggressive": 2,
            "growth": 0,
            "income": 1,
            "balanced": 2,
        }

        age = features["age"]
        risk = mapping[features["risk_tolerance"]]  
        goal = mapping[features["investment_goal"]]
        income = self.income_mapping[features["annual_income"]]
        stocks = features["stocks_current"]
        bonds = features["bonds_current"]
        crypto = features["crypto_current"]
        sentiment = features["sentiment_score"]

        input_vector = pd.DataFrame([{
            "risk_tolerance": risk,
            "age": age,
            "investment_goal": goal,
            "sentiment_score": sentiment,
            "stocks_current": stocks,
            "annual_income": income,
            "bonds_current": bonds,
            "crypto_current": crypto
        }])
        prediction = self.model.predict(input_vector)[0]
        total = sum(prediction)
        normalized = [round(x / total, 2) for x in prediction]

        return {
            "stocks": normalized[0],
            "bonds": normalized[1],
            "crypto": normalized[2]
        }

