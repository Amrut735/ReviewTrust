"""
ReviewTrust AI – Model Training Pipeline
=========================================
Dataset columns (from Kaggle "fake reviews dataset"):
  text_  → review text
  label  → CG (Computer Generated = fake) | OR (Original = genuine)

Run:
    cd backend
    python train_model.py
"""

import os
import pandas as pd
from pathlib import Path
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.model_selection import train_test_split
from sklearn.tree import DecisionTreeClassifier
from sklearn.ensemble import AdaBoostClassifier
from sklearn.metrics import accuracy_score, classification_report
import joblib

# ─── Paths (relative to this file's location) ────────────────────────────────
BASE_DIR       = Path(__file__).parent
DATASET_PATH   = BASE_DIR / "dataset" / "reviews.csv"
MODELS_DIR     = BASE_DIR / "models"
MODEL_PATH     = MODELS_DIR / "fake_review_model.pkl"
VECTORIZER_PATH = MODELS_DIR / "vectorizer.pkl"


def load_dataset() -> pd.DataFrame:
    print(f"[1/5] Loading dataset from: {DATASET_PATH}")
    df = pd.read_csv(DATASET_PATH)

    print(f"      Raw rows: {len(df):,}  |  Columns: {list(df.columns)}")

    # Drop rows with missing text or label
    df = df.dropna(subset=["text_", "label"])

    # Map CG → fake, OR → genuine (drop unrecognised labels)
    label_map = {"CG": "fake", "OR": "genuine"}
    df["label"] = df["label"].map(label_map)
    df = df.dropna(subset=["label"])

    print(f"      Clean rows: {len(df):,}")
    print(f"      Label distribution:\n{df['label'].value_counts().to_string()}\n")
    return df


def build_features(df: pd.DataFrame):
    print("[2/5] Vectorising review text with TF-IDF …")
    vectorizer = TfidfVectorizer(
        stop_words="english",
        max_features=15_000,
        ngram_range=(1, 2),   # unigrams + bigrams for richer features
        sublinear_tf=True,    # log-scale TF to dampen raw counts
    )

    X = df["text_"].astype(str)
    y = df["label"]

    X_vec = vectorizer.fit_transform(X)
    print(f"      Feature matrix shape: {X_vec.shape}\n")
    return X_vec, y, vectorizer


def split_data(X_vec, y):
    print("[3/5] Splitting  80 % train / 20 % test (random_state=42) …")
    X_train, X_test, y_train, y_test = train_test_split(
        X_vec, y, test_size=0.2, random_state=42, stratify=y
    )
    print(f"      Train: {X_train.shape[0]:,} samples  |  Test: {X_test.shape[0]:,} samples\n")
    return X_train, X_test, y_train, y_test


def train_and_evaluate(X_train, X_test, y_train, y_test):
    print("[4/5] Training models …\n")

    # ── Decision Tree ──────────────────────────────────────────────────────────
    print("  ▸ DecisionTreeClassifier")
    dt = DecisionTreeClassifier(random_state=42, max_depth=20)
    dt.fit(X_train, y_train)
    dt_preds  = dt.predict(X_test)
    dt_acc    = accuracy_score(y_test, dt_preds)
    print(f"    Accuracy : {dt_acc:.4f}  ({dt_acc * 100:.2f} %)")
    print(classification_report(y_test, dt_preds, digits=4))

    # ── AdaBoost ──────────────────────────────────────────────────────────────
    print("  ▸ AdaBoostClassifier  (n_estimators=100)")
    ada = AdaBoostClassifier(
        estimator=DecisionTreeClassifier(max_depth=3),
        n_estimators=100,
        learning_rate=0.5,
        random_state=42,
    )
    ada.fit(X_train, y_train)
    ada_preds = ada.predict(X_test)
    ada_acc   = accuracy_score(y_test, ada_preds)
    print(f"    Accuracy : {ada_acc:.4f}  ({ada_acc * 100:.2f} %)")
    print(classification_report(y_test, ada_preds, digits=4))

    print(f"\n  Decision Tree accuracy : {dt_acc * 100:.2f} %")
    print(f"  AdaBoost accuracy      : {ada_acc * 100:.2f} %")
    print("\n  ✔ Selected model: AdaBoostClassifier\n")

    return ada


def save_artifacts(model, vectorizer):
    print("[5/5] Saving model and vectorizer …")
    MODELS_DIR.mkdir(parents=True, exist_ok=True)
    joblib.dump(model,      MODEL_PATH)
    joblib.dump(vectorizer, VECTORIZER_PATH)
    print(f"      Model      → {MODEL_PATH}")
    print(f"      Vectorizer → {VECTORIZER_PATH}")
    print("\n✅  Training complete.  Start the API with:")
    print("      uvicorn app:app --reload\n")


def main():
    print("=" * 60)
    print("  ReviewTrust AI – Fake Review Detector – Model Training")
    print("=" * 60 + "\n")

    df                              = load_dataset()
    X_vec, y, vectorizer            = build_features(df)
    X_train, X_test, y_train, y_test = split_data(X_vec, y)
    model                           = train_and_evaluate(X_train, X_test, y_train, y_test)
    save_artifacts(model, vectorizer)


if __name__ == "__main__":
    main()
