from typing import List, Dict, Any
from collections import defaultdict
import logging

logger = logging.getLogger("spike_detector")

def detect_spikes(reviews: List[Dict[str, Any]]) -> Dict[str, Any]:
    """
    Platform-aware chronological spike detection.
    Amazon uses YYYY-MM-DD grouping.
    Flipkart uses YYYY-MM grouping.
    """
    if not reviews:
         return {
            "granularity": "unknown",
            "timeline": [],
            "spike_periods": [],
            "spike_detected": False,
            "platform": "unknown"
         }

    platform = reviews[0].get("platform", "unknown")
    total = len(reviews)
    valid_dates = [r["review_date"] for r in reviews if r.get("review_date")]
    
    if not valid_dates:
        return {
            "granularity": "day" if platform == "amazon" else "month",
            "timeline": [],
            "spike_periods": [],
            "spike_detected": False,
            "platform": platform,
            "data_limitation": "No date information available in reviews"
        }

    # Grouping logic
    counts = defaultdict(int)
    for date_str in valid_dates:
        counts[date_str] += 1
        
    timeline = [{"period": k, "count": v} for k, v in sorted(counts.items())]
    periods_count = len(timeline)
    avg_count = total / (periods_count if periods_count > 0 else 1)
    
    spike_periods = []
    
    if platform == "amazon":
         # Amazon: Day granularity
         # Spike = >10% of total OR (>=5 reviews AND >3x average)
         for entry in timeline:
             count = entry["count"]
             if count > (0.10 * total) or (count >= 5 and count > 3 * avg_count):
                 spike_periods.append(entry["period"])

         return {
             "granularity": "day",
             "timeline": timeline,
             "spike_periods": spike_periods,
             "spike_detected": len(spike_periods) > 0,
             "platform": platform
         }

    elif platform == "flipkart":
         # Flipkart: Month granularity
         # Spike = >15% of total OR (>=5 reviews AND >3x average)
         for entry in timeline:
             count = entry["count"]
             if count > (0.15 * total) or (count >= 5 and count > 3 * avg_count):
                 spike_periods.append(entry["period"])
                 
         return {
             "granularity": "month",
             "timeline": timeline,
             "spike_periods": spike_periods,
             "spike_detected": len(spike_periods) > 0,
             "platform": platform,
             "data_limitation": "Flipkart only provides month-level dates"
         }

    # Generic Fallback
    for entry in timeline:
        count = entry["count"]
        if count > (0.15 * total) or (count >= 5 and count > 3 * avg_count):
             spike_periods.append(entry["period"])
             
    return {
        "granularity": "unknown",
        "timeline": timeline,
        "spike_periods": spike_periods,
        "spike_detected": len(spike_periods) > 0,
        "platform": platform
    }
