"""
ReviewTrust AI – LIME Explanation Module
==========================================
Provides human-readable explanations for why a review was classified
as fake or genuine, using LIME (Local Interpretable Model-agnostic
Explanations).

Usage (from app.py):
    from explain import explain_reviews
    explained = explain_reviews(predictions)
"""

import logging
from typing import List, Dict, Tuple
from concurrent.futures import ThreadPoolExecutor

import numpy as np
from lime.lime_text import LimeTextExplainer

from model import model, vectorizer

logger = logging.getLogger("explain")

# ─── LIME setup ──────────────────────────────────────────────────────────────
# class_names order must match model.classes_
_CLASS_NAMES = list(model.classes_)
explainer = LimeTextExplainer(class_names=_CLASS_NAMES)


def _predict_proba(texts: List[str]) -> np.ndarray:
    """Bridge between LIME (passes raw text list) and our sklearn pipeline."""
    vectors = vectorizer.transform(texts)
    return model.predict_proba(vectors)


# ─── Keyword → human-readable reason mapping ────────────────────────────────
# LIME returns (word, weight) pairs.  We translate the dominant signal
# into plain-English reasons the Android app can display.

_PROMO_WORDS = {
    "amazing", "awesome", "best", "excellent", "fantastic", "great",
    "love", "loved", "perfect", "superb", "wonderful", "wow",
    "highly", "must", "buy", "recommend", "outstanding", "incredible",
    "brilliant", "fabulous", "exceptional", "magnificent",
}

_NEGATIVE_WORDS = {
    "bad", "worst", "terrible", "horrible", "poor", "awful", "waste",
    "useless", "disappointed", "disappointing", "defective", "broken",
    "cheap", "flimsy", "garbage", "rubbish", "scam", "fraud",
}

_BALANCED_WORDS = {
    "but", "however", "although", "though", "while", "decent",
    "average", "okay", "ok", "moderate", "fair", "mixed",
}


def _words_to_reasons(
    lime_features: List[tuple],
    review_text: str,
    predicted_label: str,
) -> List[str]:
    """
    Convert LIME feature weights into ≤ 3 human-readable reason strings.
    """
    reasons: List[str] = []
    words_lower = {w.lower() for w in review_text.split()}
    feature_words = {w.lower() for w, _ in lime_features}

    # 1. Review length signal
    word_count = len(review_text.split())
    if word_count < 10:
        reasons.append("Very short review length")
    elif word_count > 80:
        reasons.append("Detailed and lengthy review")

    # 2. Promotional / strong-sentiment signal
    promo_hits = feature_words & _PROMO_WORDS
    neg_hits = feature_words & _NEGATIVE_WORDS
    balanced_hits = words_lower & _BALANCED_WORDS

    if promo_hits and predicted_label == "fake":
        reasons.append("Excessive promotional language detected")
    elif promo_hits and predicted_label == "genuine":
        reasons.append("Positive sentiment with specific detail")

    if neg_hits and predicted_label == "fake":
        reasons.append("Exaggerated negative language")
    elif neg_hits and predicted_label == "genuine":
        reasons.append("Critical feedback with specifics")

    # 3. Balanced / nuanced
    if balanced_hits:
        reasons.append("Balanced sentiment with pros and cons")

    # 4. Vocabulary diversity
    unique_ratio = len(set(words_lower)) / max(len(words_lower), 1)
    if unique_ratio < 0.5 and predicted_label == "fake":
        reasons.append("Low vocabulary diversity")
    elif unique_ratio > 0.7 and predicted_label == "genuine":
        reasons.append("Rich and varied vocabulary")

    # Ensure we always return at least one reason
    if not reasons:
        if predicted_label == "fake":
            reasons.append("Pattern resembles computer-generated text")
        else:
            reasons.append("Pattern consistent with authentic user experience")

    return reasons[:3]


def explain_review(review_text: str, predicted_label: str) -> List[str]:
    """
    Generate human-readable explanation for a single review prediction.

    Returns up to 3 reason strings.
    """
    try:
        exp = explainer.explain_instance(
            review_text,
            _predict_proba,
            num_features=5,
            num_samples=50,      # keep small for speed
        )
        lime_features = exp.as_list()
        return _words_to_reasons(lime_features, review_text, predicted_label)
    except Exception as exc:
        logger.warning("LIME explanation failed: %s", exc)
        if predicted_label == "fake":
            return ["Pattern resembles computer-generated text"]
        return ["Pattern consistent with authentic user experience"]


def explain_reviews(
    predictions: List[Dict[str, str]],
    max_fake: int = 10,
    max_genuine: int = 10,
) -> Dict[str, List[Dict]]:
    """
    Separate predictions into fake / genuine buckets and generate LIME
    explanations for the top reviews in each bucket.

    Parameters
    ----------
    predictions : list of {"review": str, "prediction": "fake"|"genuine"}
    max_fake    : max fake reviews to explain  (default 10)
    max_genuine : max genuine reviews to explain (default 10)

    Returns
    -------
    {
        "fake_review_details":    [{"text": ..., "explanation": [...]}, ...],
        "genuine_review_details": [{"text": ..., "explanation": [...]}, ...],
    }
    """
    fake_texts = [p["review"] for p in predictions if p["prediction"] == "fake"]
    genuine_texts = [p["review"] for p in predictions if p["prediction"] == "genuine"]

    # Pick the reviews to explain (first N — they're already in scrape order)
    fake_to_explain = fake_texts[:max_fake]
    genuine_to_explain = genuine_texts[:max_genuine]

    logger.info(
        "Explaining %d fake + %d genuine reviews with LIME (parallel)",
        len(fake_to_explain), len(genuine_to_explain),
    )

    # Build (text, label) pairs for parallel processing
    tasks: List[Tuple[str, str]] = (
        [(t, "fake") for t in fake_to_explain] +
        [(t, "genuine") for t in genuine_to_explain]
    )

    def _explain_task(pair: Tuple[str, str]) -> Dict:
        text, label = pair
        reasons = explain_review(text, label)
        return {"text": text, "explanation": reasons}

    # Run LIME explanations in parallel (each is CPU-bound but releases GIL
    # for numpy/sklearn C extensions, so threads still help significantly)
    results: List[Dict] = []
    if tasks:
        with ThreadPoolExecutor(max_workers=min(4, len(tasks))) as pool:
            results = list(pool.map(_explain_task, tasks))

    n_fake = len(fake_to_explain)
    fake_details = results[:n_fake]
    genuine_details = results[n_fake:]

    return {
        "fake_review_details": fake_details,
        "genuine_review_details": genuine_details,
    }
