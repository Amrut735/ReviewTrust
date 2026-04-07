"""
ReviewTrust AI – Trust Score Calculator
=========================================
Pure function; no ML dependency.
"""

from typing import Dict, List


def calculate_trust_score(predictions: List[Dict[str, str]]) -> Dict:
    """
    Derive a product trust score from per-review predictions.

    Formula
    -------
        Trust Score = (Genuine Reviews / Total Reviews) × 100

    Parameters
    ----------
    predictions : output of model.predict_reviews()
        e.g. [{"review": "...", "prediction": "genuine"}, ...]

    Returns
    -------
    dict with keys:
        trust_score       (int  0–100)
        fake_reviews      (int)
        genuine_reviews   (int)
        total_reviews     (int)
        fake_percentage   (int  0–100)
        genuine_percentage (int  0–100)
    """
    total = len(predictions)

    if total == 0:
        return {
            "trust_score":        0,
            "fake_count":         0,
            "genuine_count":      0,
            "total_reviews":      0,
            "fake_percentage":    0,
            "genuine_percentage": 0,
            "recommendation":    "Not enough data to make a recommendation.",
        }

    genuine_count = sum(1 for p in predictions if p["prediction"] == "genuine")
    fake_count    = total - genuine_count

    genuine_pct = round((genuine_count / total) * 100)
    fake_pct    = 100 - genuine_pct          # guarantees they always sum to 100

    # Purchase recommendation based on review authenticity analysis
    if genuine_pct >= 80:
        recommendation = "Reviews appear trustworthy. Safe to consider buying."
    elif genuine_pct >= 60:
        recommendation = "Mixed review authenticity detected. Proceed with caution."
    else:
        recommendation = "Many suspicious reviews detected. Consider skipping this product."

    return {
        "trust_score":        genuine_pct,
        "fake_count":         fake_count,
        "genuine_count":      genuine_count,
        "total_reviews":      total,
        "fake_percentage":    fake_pct,
        "genuine_percentage": genuine_pct,
        "recommendation":    recommendation,
    }
