"""
ReviewTrust AI – FastAPI Backend
==================================
Start:
    cd backend
    uvicorn app:app --reload

Endpoint consumed by the Android app:
    POST /analyze   {"product_url": "https://...", "analysis_mode": "normal"}
"""

import time
import hashlib
import json
import logging
import re
from pathlib import Path
from typing import Optional
from urllib.parse import urlparse, urlunparse

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

from scraper     import scrape_reviews, _resolve_url
from model       import predict_reviews
from trust_score import calculate_trust_score
from explain     import explain_reviews
from sentiment   import analyze_sentiment
from reviewer_quality import score_reviewers
from spike_detector import detect_spikes
from summarizer  import summarize_genuine

from concurrent.futures import ThreadPoolExecutor

# ─── App setup ────────────────────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s  %(levelname)-8s  %(name)s  %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger("app")

app = FastAPI(
    title="ReviewTrust AI",
    description="Fake product-review detection API",
    version="2.0.0",
)

# Allow all origins so the Android emulator / device can reach the dev server
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


# ─── URL canonicalization (ensures same product → same cache key) ─────────────
_SHORT_LINK_HOSTS = {"dl.flipkart.com", "amzn.in", "amzn.com"}
# Query params to strip — these are tracking / app metadata, not product identity
_STRIP_PARAMS_RE = re.compile(
    r'[&?](?:_refId|_appId|param|tracker|cmpid|srcContext|ssid|otracker'
    r'|store|qH|collection-tab-name|lid|fm|iid|ppt|ppn|lsId|lsSource'
    r'|mc|searchQuery)?=[^&]*', re.IGNORECASE
)

def _canonical_url(url: str) -> str:
    """
    Turn any product URL into a stable canonical form for caching.
    - Resolves short / share links (dl.flipkart.com, amzn.in)
    - Strips tracking query params
    - Lowercases host
    """
    parsed = urlparse(url.strip())
    host = (parsed.hostname or "").lower()

    # Resolve short / share links first (follows redirects, ~1-2 s)
    if host in _SHORT_LINK_HOSTS:
        url = _resolve_url(url.strip())
        logger.info("Canonical resolve: %s", url)

    # Strip tracking params
    clean = _STRIP_PARAMS_RE.sub("", url)
    # Fix leading ? becoming & after stripping
    clean = re.sub(r'\?&', '?', clean)
    # Remove trailing ? if all params were stripped
    clean = re.sub(r'\?$', '', clean)

    # Lowercase the host part only
    p = urlparse(clean)
    canonical = urlunparse((
        p.scheme,
        p.netloc.lower(),
        p.path,
        p.params,
        p.query,
        '',  # drop fragment
    ))
    logger.info("Canonical URL: %s", canonical)
    return canonical


# ─── File-based cache (normal mode only, survives server restarts) ────────────
_CACHE_DIR = Path(__file__).parent / "cache"
_CACHE_DIR.mkdir(exist_ok=True)
CACHE_TTL = 7 * 24 * 60 * 60           # 1 week in seconds

def _cache_key(url: str) -> str:
    return hashlib.sha256(url.strip().lower().encode()).hexdigest()

def _cache_path(url: str) -> Path:
    return _CACHE_DIR / f"{_cache_key(url)}.json"

def _get_cached(url: str) -> Optional[dict]:
    path = _cache_path(url)
    if not path.exists():
        return None
    try:
        raw = json.loads(path.read_text(encoding="utf-8"))
        if (time.time() - raw.get("ts", 0)) < CACHE_TTL:
            logger.info("Cache HIT for %s", url)
            return raw["data"]
        # expired
        path.unlink(missing_ok=True)
    except Exception as exc:
        logger.warning("Cache read error for %s: %s", url, exc)
        path.unlink(missing_ok=True)
    return None

def _set_cache(url: str, data: dict) -> None:
    path = _cache_path(url)
    try:
        path.write_text(
            json.dumps({"data": data, "ts": time.time()}, ensure_ascii=False),
            encoding="utf-8",
        )
        logger.info("Cache SET for %s → %s", url, path.name)
    except Exception as exc:
        logger.warning("Cache write error: %s", exc)


# ─── Analysis mode constants ─────────────────────────────────────────────────
_NORMAL_MAX_REVIEWS   = 60
_NORMAL_MAX_FAKE_EXP  = 5
_NORMAL_MAX_GEN_EXP   = 5
_DEEP_MAX_FAKE_EXP    = 20
_DEEP_MAX_GEN_EXP     = 20


# ─── Request / Response schemas ───────────────────────────────────────────────
class AnalyzeRequest(BaseModel):
    product_url:   str                # kept as plain str so any URL parses
    analysis_mode: str = "normal"     # "normal" | "deep"


class ReviewDetail(BaseModel):
    text:        str
    explanation: list[str]


class AnalyzeResponse(BaseModel):
    trust_score:            int
    fake_percentage:        int
    genuine_percentage:     int
    total_reviews:          int
    fake_count:             int
    genuine_count:          int
    recommendation:         str
    analysis_mode:          str = "normal"
    cached:                 bool = False
    fake_reviews:           list[ReviewDetail] = []
    genuine_reviews:        list[ReviewDetail] = []
    sentiment:              Optional[dict] = None
    reviewer_quality:       Optional[dict] = None
    spike_detection:        Optional[dict] = None
    genuine_summary:        Optional[dict] = None


# ─── Health check ─────────────────────────────────────────────────────────────
@app.get("/")
def root():
    return {"status": "ok", "service": "ReviewTrust AI Backend"}


@app.get("/health")
def health():
    return {"status": "healthy"}


# ─── Main endpoint ────────────────────────────────────────────────────────────
@app.post("/analyze", response_model=AnalyzeResponse)
def analyze(request: AnalyzeRequest):
    """
    Full pipeline:
      1. (Normal mode) Check cache — return immediately if fresh
      2. Scrape reviews (capped to 60 in normal mode)
      3. Classify each review (fake | genuine)
      4. Calculate trust score
      5. Generate LIME explanations (top 5 in normal, top 20 in deep)
      6. Return aggregated stats + categorised review lists
    """
    url  = request.product_url.strip()
    mode = request.analysis_mode.strip().lower()
    if mode not in ("normal", "deep"):
        mode = "normal"

    logger.info("▶ /analyze  mode=%s  url=%s", mode, url)
    t0 = time.time()

    # ── Step 0: Canonicalize URL & cache check ────────────────────────────────
    canon = _canonical_url(url)      # resolves share links, strips tracking params
    if mode == "normal":
        cached_data = _get_cached(canon)
        if cached_data:
            cached_data["cached"] = True
            logger.info("   Returning cached result in %.1fs", time.time() - t0)
            return cached_data

    # ── Step 1: Scrape ────────────────────────────────────────────────────────
    max_reviews = _NORMAL_MAX_REVIEWS if mode == "normal" else 0
    reviews = scrape_reviews(url, max_reviews=max_reviews)
    logger.info("   Reviews scraped : %d  (%.1fs)", len(reviews), time.time() - t0)

    if not reviews:
        url_lower = url.lower()
        if "dl.flipkart.com/s/" in url_lower:
            hint = (
                " This is a Flipkart app share-link which is protected by "
                "reCAPTCHA and cannot be scraped directly. "
                "Open the product in a browser, copy the URL from the address bar "
                "(flipkart.com/product-name/p/itm...), and share that instead."
            )
        elif "amzn.in" in url_lower or "amzn.com" in url_lower:
            hint = (
                " The Amazon short-link may have expired. "
                "Share the full product page URL (amazon.in/dp/ASIN) instead."
            )
        else:
            hint = (
                " The product page may be blocking automated requests "
                "or the URL is not a supported Amazon / Flipkart product page."
            )
        raise HTTPException(status_code=422, detail="Could not scrape reviews." + hint)

    # ── Step 2: Predict ───────────────────────────────────────────────────────
    predictions = predict_reviews(reviews)
    logger.info(
        "   Predictions: fake=%d  genuine=%d  (%.1fs)",
        sum(1 for p in predictions if p["prediction"] == "fake"),
        sum(1 for p in predictions if p["prediction"] == "genuine"),
        time.time() - t0,
    )

    # ── Inject predictions back into reviews as 'label' for advanced modules ──
    for rev, pred in zip(reviews, predictions):
        rev["label"] = pred["prediction"]

    # ── Advanced Analysis Modules (sequential) ──────────────────────────────────
    try:
        res_sentiment = analyze_sentiment(reviews)
    except Exception as e:
        logger.error("Sentiment analysis failed: %s", e)
        res_sentiment = None

    try:
        res_quality = score_reviewers(reviews)
    except Exception as e:
        logger.error("Reviewer quality analysis failed: %s", e)
        res_quality = None

    try:
        res_spikes = detect_spikes(reviews)
    except Exception as e:
        logger.error("Spike detection failed: %s", e)
        res_spikes = None

    try:
        res_summary = summarize_genuine(reviews)
    except Exception as e:
        logger.error("Summarizer failed: %s", e)
        res_summary = None

    # ── Step 3: Trust score ───────────────────────────────────────────────────
    result = calculate_trust_score(predictions)
    result["analysis_mode"] = mode
    logger.info(
        "   Trust score : %d %%  (fake %d %% | genuine %d %%)",
        result["trust_score"], result["fake_percentage"], result["genuine_percentage"],
    )

    # ── Step 4: LIME explanations (parallel) ──────────────────────────────────
    if mode == "normal":
        max_f, max_g = _NORMAL_MAX_FAKE_EXP, _NORMAL_MAX_GEN_EXP
    else:
        max_f, max_g = _DEEP_MAX_FAKE_EXP, _DEEP_MAX_GEN_EXP

    explained = explain_reviews(predictions, max_fake=max_f, max_genuine=max_g)
    result["fake_reviews"]    = explained["fake_review_details"]
    result["genuine_reviews"] = explained["genuine_review_details"]

    elapsed = time.time() - t0
    logger.info(
        "   Explained: %d fake + %d genuine review details  (total %.1fs)",
        len(result["fake_reviews"]), len(result["genuine_reviews"]), elapsed,
    )

    result["sentiment"] = res_sentiment
    result["reviewer_quality"] = res_quality
    result["spike_detection"] = res_spikes
    result["genuine_summary"] = res_summary

    # ── Step 5: Cache result (normal mode only) ───────────────────────────────
    result["cached"] = False
    if mode == "normal":
        _set_cache(canon, result)

    return result
