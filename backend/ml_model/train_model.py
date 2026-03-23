import pandas as pd
import joblib
import os

from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import accuracy_score, classification_report
from xgboost import XGBClassifier

print("Loading dataset...")

# -------------------------
# Load updated dataset
# -------------------------
df = pd.read_csv("dataset/copd_dataset_updated.csv")

# -------------------------
# Clean column names
# -------------------------
df.columns = df.columns.str.strip().str.replace(" ", "_")

# -------------------------
# Encode target
# -------------------------
le = LabelEncoder()
df["Recommended_Device"] = le.fit_transform(df["Recommended_Device"])

# Show mapping
print("\nDevice Mapping:")
for i, label in enumerate(le.classes_):
    print(f"{i} → {label}")

# -------------------------
# Split dataset
# -------------------------
X = df.drop("Recommended_Device", axis=1)
y = df["Recommended_Device"]

X_train, X_test, y_train, y_test = train_test_split(
    X, y,
    test_size=0.2,
    random_state=42,
    stratify=y
)

print("\nTraining model...")

# -------------------------
# Model
# -------------------------
model = XGBClassifier(
    n_estimators=500,
    max_depth=6,
    learning_rate=0.05,
    eval_metric="mlogloss",
    tree_method="hist",
    n_jobs=-1
)

model.fit(X_train, y_train)

# -------------------------
# Evaluate
# -------------------------
pred = model.predict(X_test)

print("\nAccuracy:", accuracy_score(y_test, pred))
print("\nClassification Report:\n")
print(classification_report(y_test, pred))

# -------------------------
# Save model
# -------------------------
os.makedirs("trained_model", exist_ok=True)

joblib.dump(model, "trained_model/model.pkl")
joblib.dump(le, "trained_model/encoder.pkl")

print("\n✅ Model saved successfully!")