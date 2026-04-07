from typing import List, Dict, Any
import logging

logger = logging.getLogger("reviewer_quality")

def score_reviewers(reviews: List[Dict[str, Any]]) -> Dict[str, Any]:
    """
    Platform-aware logic to score reviewer quality based on metadata.
    """
    if not reviews:
        return {
            "platform": "unknown",
            "suspicious_reviewer_pct": 0.0,
            "unverified_pct": 0.0,
            "thin_account_count": 0,
            "mass_reviewer_count": 0,
            "data_limitation": "No reviews available"
        }

    platform = reviews[0].get("platform", "unknown")
    total = len(reviews)
    suspicious_count = 0
    unverified_count = 0
    thin_count = 0
    mass_count = 0

    if platform == "amazon":
        for rev in reviews:
            sus = False
            verified = rev.get("verified_purchase", False)
            if not verified:
                unverified_count += 1
                sus = True

            total_revs = rev.get("reviewer_total_reviews")
            if total_revs is not None:
                if total_revs < 3:
                    thin_count += 1
                    sus = True
                elif total_revs > 200:
                    mass_count += 1
                    sus = True
            
            if sus:
                suspicious_count += 1

        return {
            "platform": platform,
            "suspicious_reviewer_pct": float(round((suspicious_count / total) * 100, 2)),
            "unverified_pct": float(round((unverified_count / total) * 100, 2)),
            "thin_account_count": thin_count,
            "mass_reviewer_count": mass_count
        }

    elif platform == "flipkart":
        for rev in reviews:
            sus = False
            verified = rev.get("verified_purchase", False)
            if not verified:
                unverified_count += 1
                sus = True
            
            if sus:
                suspicious_count += 1

        return {
            "platform": platform,
            "suspicious_reviewer_pct": float(round((suspicious_count / total) * 100, 2)),
            "unverified_pct": float(round((unverified_count / total) * 100, 2)),
            "thin_account_count": 0,
            "mass_reviewer_count": 0,
            "data_limitation": "Flipkart does not expose reviewer history"
        }
    
    # Generic fallback
    for rev in reviews:
        if not rev.get("verified_purchase", False):
            unverified_count += 1
            suspicious_count += 1
            
    return {
        "platform": platform,
        "suspicious_reviewer_pct": float(round((suspicious_count / total) * 100, 2)),
        "unverified_pct": float(round((unverified_count / total) * 100, 2)),
        "thin_account_count": 0,
        "mass_reviewer_count": 0,
        "data_limitation": f"{platform.capitalize()} reviewer history not extracted"
    }
