from typing import List, Dict, Any
from vaderSentiment.vaderSentiment import SentimentIntensityAnalyzer
import logging

logger = logging.getLogger("sentiment")
_analyzer = SentimentIntensityAnalyzer()

def analyze_sentiment(reviews: List[Dict[str, Any]]) -> Dict[str, Any]:
    """
    Run sentiment analysis ONLY on reviews where label == 'genuine'.
    Uses VADER (SentimentIntensityAnalyzer).
    """
    genuine_reviews = [r for r in reviews if r.get("label") == "genuine"]
    
    if not genuine_reviews:
        return {
            "positive_pct": 0.0,
            "negative_pct": 0.0,
            "neutral_pct": 0.0,
            "average_compound": 0.0
        }

    total = len(genuine_reviews)
    positive_count = 0
    negative_count = 0
    neutral_count = 0
    compound_sum = 0.0

    for rev in genuine_reviews:
        text = rev.get("review_text", "")
        # VADER returns polarity scores including 'compound' (-1.0 to 1.0)
        scores = _analyzer.polarity_scores(text)
        compound = scores["compound"]
        compound_sum += compound

        if compound >= 0.05:
            positive_count += 1
        elif compound <= -0.05:
            negative_count += 1
        else:
            neutral_count += 1

    return {
        "positive_pct": float(round((positive_count / total) * 100, 2)),
        "negative_pct": float(round((negative_count / total) * 100, 2)),
        "neutral_pct": float(round((neutral_count / total) * 100, 2)),
        "average_compound": float(round(compound_sum / total, 3))
    }
