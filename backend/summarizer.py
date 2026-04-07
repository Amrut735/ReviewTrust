from typing import List, Dict, Any
import logging
import nltk

try:
    nltk.download('punkt', quiet=True)
    nltk.download('punkt_tab', quiet=True)
except Exception as e:
    pass

# Set up sumy
from sumy.parsers.plaintext import PlaintextParser
from sumy.nlp.tokenizers import Tokenizer
from sumy.summarizers.lsa import LsaSummarizer
from sumy.nlp.stemmers import Stemmer
from sumy.utils import get_stop_words

logger = logging.getLogger("summarizer")

def summarize_genuine(reviews: List[Dict[str, Any]], max_sentences: int = 3) -> Dict[str, Any]:
    """
    Summarize genuine reviews using sumy LSA.
    Requires at least 5 genuine reviews to operate.
    """
    genuine_texts = [
        r.get("review_text", "").strip() 
        for r in reviews 
        if r.get("label") == "genuine" and r.get("review_text")
    ]

    count = len(genuine_texts)
    
    if count < 5:
        return {
            "summary": None,
            "reason": "insufficient genuine reviews",
            "genuine_review_count_used": count,
            "max_sentences": max_sentences
        }

    # Combine all review texts
    combined_text = ". ".join(genuine_texts)
    
    try:
        parser = PlaintextParser.from_string(combined_text, Tokenizer("english"))
        stemmer = Stemmer("english")
        
        summarizer = LsaSummarizer(stemmer)
        summarizer.stop_words = get_stop_words("english")
        
        output_sentences = summarizer(parser.document, max_sentences)
        summary = " ".join(str(s) for s in output_sentences)
        
        return {
            "summary": summary,
            "genuine_review_count_used": count,
            "max_sentences": max_sentences
        }
    except Exception as e:
        logger.error(f"Summarizer failed: {e}")
        return {
            "summary": None,
            "reason": "summarization failed internally",
            "genuine_review_count_used": count,
            "max_sentences": max_sentences
        }
