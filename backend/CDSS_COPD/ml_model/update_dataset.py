import pandas as pd
import numpy as np

print("Loading dataset...")

# Load dataset
df = pd.read_csv("dataset/copd_dataset.csv")

# Generate conflict samples
def generate_conflict_samples(n=500):

    df_conflict = pd.DataFrame()

    df_conflict["SpO2"] = np.random.randint(80, 88, n)
    df_conflict["pH"] = np.round(np.random.uniform(7.20, 7.35, n), 2)
    df_conflict["PaCO2"] = np.random.randint(46, 61, n)

    df_conflict["PaO2"] = np.random.randint(50, 80, n)
    df_conflict["HCO3"] = np.random.randint(24, 32, n)
    df_conflict["Respiratory_Rate"] = np.random.randint(20, 35, n)
    df_conflict["Heart_Rate"] = np.random.randint(80, 120, n)

    df_conflict["Recommended_Device"] = "Venturi Mask"

    return df_conflict

print("Adding conflict samples...")

conflict_df = generate_conflict_samples(500)

df = pd.concat([df, conflict_df], ignore_index=True)

# Shuffle dataset
df = df.sample(frac=1, random_state=42).reset_index(drop=True)

# Save updated dataset
df.to_csv("dataset/copd_dataset_updated.csv", index=False)

print("✅ Dataset updated successfully!")