import pandas as pd
import numpy as np

# Generate synthetic data
np.random.seed(42)
n_samples = 1000

# Added income brackets (in thousands)
income_brackets = ["<50", "50-100", "100-200", ">200"]


data = {
    "age": np.random.randint(18, 70, n_samples),
    "annual_income": np.random.choice(income_brackets, n_samples),
    "risk_tolerance": np.random.choice(["conservative", "moderate", "aggressive"], n_samples),
    "investment_goal": np.random.choice(["growth", "income", "balanced"], n_samples),
    "stocks_current": np.random.uniform(0.1, 0.8, n_samples),
    "bonds_current": np.random.uniform(0.1, 0.7, n_samples),
    "crypto_current": np.random.uniform(0.0, 0.3, n_samples),
    "sentiment_score": np.random.uniform(0.2, 0.9, n_samples)
}

df = pd.DataFrame(data)

# Step 2: Normalize current allocation to sum to 1
total_alloc = df[["stocks_current", "bonds_current", "crypto_current"]].sum(axis=1)
df["stocks_current"] /= total_alloc
df["bonds_current"] /= total_alloc
df["crypto_current"] /= total_alloc


# Step 3: Simulate label outputs
def calculate_targets(row):
    income = row["annual_income"]
    risk = row["risk_tolerance"]

    # Conservative baseline
    stocks = 0.2 + np.random.rand() * 0.1
    bonds = 0.6 + np.random.rand() * 0.1
    crypto = 0.05 + np.random.rand() * 0.05


    if risk == "moderate":
        stocks = 0.4 + np.random.rand() * 0.1
        bonds = 0.4 + np.random.rand() * 0.1
        crypto = 0.1 + np.random.rand() * 0.1
    elif risk == "aggressive":
        stocks = 0.6 + np.random.rand() * 0.1
        bonds = 0.2 + np.random.rand() * 0.1
        crypto = 0.1 + np.random.rand() * 0.1

    # Adjust allocation based on income bracket
    if income == "<50":
        # Less crypto and stocks for lower income
        stocks -= 0.05
        bonds += 0.05
        crypto = max(crypto - 0.03, 0.01)
    elif income == "50-100":
        # Slightly more balanced
        stocks += 0.02
        bonds -= 0.02
    elif income == "100-200":
        # Slight boost to stocks and crypto
        stocks += 0.03
        crypto += 0.02
    elif income == ">200":
        # High income: higher risk tolerance support
        stocks += 0.05
        crypto += 0.03
        bonds -= 0.05

    # Ensure allocations remain within [0, 1] and normalize to sum to 1
    alloc = np.array([stocks, bonds, crypto])
    alloc = np.clip(alloc, 0.01, 1.0)
    alloc /= alloc.sum()  # normalize to sum to 1

    return alloc.tolist()


# Step 4: Apply target generation
allocations = np.array([calculate_targets(row) for _, row in df.iterrows()])
allocations /= allocations.sum(axis=1)[:, None]  # Normalize rows to sum to 1
df["stocks_target"] = allocations[:, 0]
df["bonds_target"] = allocations[:, 1]
df["crypto_target"] = allocations[:, 2]


# Step 5: Save full dataset
output_path = "demo/src/main/java/com/example/investmentplatform/python_model/enhanced_recommendation_data.csv"
df.to_csv(output_path, index=False)
print(f"Generated dataset: {output_path}")