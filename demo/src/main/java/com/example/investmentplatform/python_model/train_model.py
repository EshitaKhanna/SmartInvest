import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_absolute_error
from sklearn.ensemble import RandomForestRegressor
import joblib

df = pd.read_csv("demo/src/main/java/com/example/investmentplatform/python_model/enhanced_recommendation_data.csv")
print(df.head())
mapping = {
    # risk_tolerance
    "conservative": 0,
    "moderate": 1,
    "aggressive": 2,

    # investment_goal
    "growth": 0,
    "income": 1,
    "balanced": 2,

    # incme
    "<50": 0,
    "50-100": 1,
    "100-200": 2,
    ">200": 3
}

df["risk_tolerance"] = df["risk_tolerance"].map(mapping)
df["investment_goal"] = df["investment_goal"].map(mapping)
df["annual_income"] = df["annual_income"].map(mapping)

X = df[["risk_tolerance", "age", "investment_goal", "sentiment_score", "stocks_current", "annual_income", "bonds_current", "crypto_current"]]
y = df[["stocks_target", "bonds_target", "crypto_target"]]

# Split data
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2)

model = RandomForestRegressor(n_estimators=100, random_state=42)
model.fit(X_train, y_train)

# Validate
if hasattr(model, 'feature_names_in_'): 
    X_test = pd.DataFrame(X_test, columns=model.feature_names_in_)
preds = model.predict(X_test)
print(f"MAE: {mean_absolute_error(y_test, preds)}")

joblib.dump(model, "recommendation_model.pkl")
print("✅ Model trained and saved.")
