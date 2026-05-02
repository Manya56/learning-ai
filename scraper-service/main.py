from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from playwright.async_api import async_playwright
from bs4 import BeautifulSoup
import asyncio
import hashlib
import re
import logging
import sys
import asyncio

# Fix for Windows — Playwright requires ProactorEventLoop
if sys.platform == "win32":
    asyncio.set_event_loop_policy(asyncio.WindowsProactorEventLoopPolicy())

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="LearnAI Scraper Service", version="1.0.0")

# ── Request/Response models ───────────────────────────────────────────────────

class ScrapeRequest(BaseModel):
    url: str
    concept_tag: str
    concept_name: str = ""
    wait_for: str = ""        # CSS selector to wait for
    timeout_ms: int = 15000

class ScrapeResponse(BaseModel):
    url: str
    title: str
    body_text: str
    word_count: int
    url_hash: str
    success: bool
    error: str = ""

# ── Trusted JS-rendered domains ───────────────────────────────────────────────

JS_SITES = {
    "neetcode.io":          "article, .content, main",
    "visualgo.net":         "#wiki-group, .jssimcanvas + div",
    "developer.mozilla.org": "article.main-page-content",
    "leetcode.com":         ".question-content, .content-wrapper",
    "realpython.com":       "article.border-none",
    "roadmap.sh":           "main, article",
    "refactoring.guru":     "article.page-content",
    "cs.stanford.edu":      "main, .content",
}

# ── Health check ─────────────────────────────────────────────────────────────

@app.get("/health")
async def health():
    return {"status": "UP", "service": "playwright-scraper"}

@app.head("/health")
def health_head():
    return {"status": "UP", "service": "playwright-scraper"}

# ── Main scrape endpoint ──────────────────────────────────────────────────────

@app.post("/scrape", response_model=ScrapeResponse)
async def scrape(request: ScrapeRequest):
    logger.info(f"Scraping: {request.url}")

    try:
        result = await scrape_with_playwright(
            request.url,
            request.wait_for,
            request.timeout_ms
        )
        return result

    except Exception as e:
        logger.error(f"Scrape failed for {request.url}: {e}")
        return ScrapeResponse(
            url=request.url,
            title="",
            body_text="",
            word_count=0,
            url_hash=md5(request.url),
            success=False,
            error=str(e)
        )

# ── Batch scrape endpoint ─────────────────────────────────────────────────────

class BatchScrapeRequest(BaseModel):
    urls: list[str]
    concept_tag: str
    concept_name: str = ""

class BatchScrapeResponse(BaseModel):
    results: list[ScrapeResponse]
    success_count: int
    fail_count: int

@app.post("/scrape/batch", response_model=BatchScrapeResponse)
async def scrape_batch(request: BatchScrapeRequest):
    logger.info(f"Batch scraping {len(request.urls)} URLs")

    tasks = [
        scrape_with_playwright(url, "", 15000)
        for url in request.urls
    ]

    results = await asyncio.gather(*tasks, return_exceptions=True)

    responses = []
    for i, result in enumerate(results):
        if isinstance(result, Exception):
            responses.append(ScrapeResponse(
                url=request.urls[i],
                title="",
                body_text="",
                word_count=0,
                url_hash=md5(request.urls[i]),
                success=False,
                error=str(result)
            ))
        else:
            responses.append(result)

    success_count = sum(1 for r in responses if r.success)

    return BatchScrapeResponse(
        results=responses,
        success_count=success_count,
        fail_count=len(responses) - success_count
    )

# ── Core Playwright scraping logic ────────────────────────────────────────────

async def scrape_with_playwright(url: str,
                                  wait_for: str,
                                  timeout_ms: int) -> ScrapeResponse:
    async with async_playwright() as p:
        browser = await p.chromium.launch(
            headless=True,
            args=[
                "--no-sandbox",
                "--disable-setuid-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
            ]
        )

        context = await browser.new_context(
            user_agent=(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                "AppleWebKit/537.36 (KHTML, like Gecko) "
                "Chrome/120.0.0.0 Safari/537.36"
            ),
            viewport={"width": 1280, "height": 800}
        )

        page = await context.new_page()

        try:
            await page.route("**/*.{png,jpg,jpeg,gif,svg,ico,woff,woff2}",
                             lambda route: route.abort())
            await page.route("**/ads/**",
                             lambda route: route.abort())
            await page.route("**/analytics/**",
                             lambda route: route.abort())

            await page.goto(url,
                            wait_until="domcontentloaded",
                            timeout=timeout_ms)

            domain = extract_domain(url)

            # ── Site-specific wait strategies ─────────────────────────────
            if "neetcode.io" in domain:
                # Wait for the roadmap grid to render
                try:
                    await page.wait_for_selector(
                        "a[href*='/problems'], .roadmap, "
                        "h1, h2, .group, [class*='roadmap']",
                        timeout=10000
                    )
                except Exception:
                    pass
                # Extra wait for React hydration
                await asyncio.sleep(3)

            elif "visualgo.net" in domain:
                try:
                    await page.wait_for_selector(
                        "#wiki-group, .jssimcanvas",
                        timeout=10000
                    )
                except Exception:
                    pass
                await asyncio.sleep(2)

            elif "leetcode.com" in domain:
                try:
                    await page.wait_for_selector(
                        ".question-content, [data-key]",
                        timeout=10000
                    )
                except Exception:
                    pass
                await asyncio.sleep(2)

            elif "roadmap.sh" in domain:
                try:
                    await page.wait_for_selector(
                        "main, article, svg",
                        timeout=10000
                    )
                except Exception:
                    pass
                await asyncio.sleep(2)

            else:
                # Generic — wait for custom selector or network idle
                if wait_for:
                    try:
                        await page.wait_for_selector(
                            wait_for, timeout=8000)
                    except Exception:
                        pass
                # Always do a small sleep for React/Vue hydration
                await asyncio.sleep(2)

            html  = await page.content()
            title = await page.title()

        finally:
            await browser.close()

    body_text = extract_text(html, url)
    title     = clean_title(title)

    return ScrapeResponse(
        url=url,
        title=title,
        body_text=body_text,
        word_count=len(body_text.split()),
        url_hash=md5(url),
        success=len(body_text) > 100
    )

# ── Text extraction ───────────────────────────────────────────────────────────

def extract_text(html: str, url: str) -> str:
    soup = BeautifulSoup(html, "lxml")

    # Remove noise
    for tag in soup.select(
        "nav, header, footer, aside, script, style, "
        ".sidebar, .advertisement, .cookie-banner, "
        ".popup, .modal, #comments, .social-share, "
        ".related-posts, .newsletter-signup"
    ):
        tag.decompose()

    domain = extract_domain(url)

    # Site-specific selectors
    selectors = {
        "neetcode.io": [
            "main", "body",
            "[class*='roadmap']", "[class*='content']",
            "h1", "h2", "a[href*='/problems']"
        ],
        "visualgo.net":          ["#wiki-group", ".explanation"],
        "developer.mozilla.org": ["article.main-page-content"],
        "leetcode.com":          [".question-content"],
        "realpython.com":        ["article"],
        "roadmap.sh":            ["main", "article", "svg text"],
        "refactoring.guru":      [".page-content"],
    }

    site_selectors = selectors.get(domain, [
        "article", "main", ".content",
        ".post-content", ".entry-content",
        "#content", "div.body"
    ])

    for selector in site_selectors:
        element = soup.select_one(selector)
        if element:
            text = element.get_text(separator="\n", strip=True)
            if len(text) > 200:
                return clean_text(text)
            
    if "neetcode.io" in domain or "roadmap.sh" in domain:
        links = soup.find_all("a", href=True)
        link_texts = [a.get_text(strip=True) for a in links
                      if len(a.get_text(strip=True)) > 2]
        if len(link_texts) > 10:
            return clean_text("\n".join(link_texts))

    # Fallback — get all paragraph text
    paragraphs = soup.find_all(["p", "h1", "h2", "h3",
                                 "h4", "li", "code", "pre"])
    text = "\n".join(p.get_text(strip=True) for p in paragraphs)
    return clean_text(text)

def clean_text(text: str) -> str:
    if not text:
        return ""
    # Remove HTML entities
    text = re.sub(r"&[a-zA-Z]+;", " ", text)
    # Collapse whitespace
    text = re.sub(r"[ \t]+", " ", text)
    # Max 2 newlines
    text = re.sub(r"\n{3,}", "\n\n", text)
    # Remove lines that are only punctuation
    text = re.sub(r"(?m)^[^a-zA-Z]*$\n?", "", text)
    return text.strip()[:50000]  # Max 50K chars

def clean_title(title: str) -> str:
    if not title:
        return "Untitled"
    # Remove site name suffix
    return re.sub(r"\s*[-|]\s*[^-|]+$", "", title).strip()

def extract_domain(url: str) -> str:
    try:
        from urllib.parse import urlparse
        host = urlparse(url).hostname or ""
        return host.replace("www.", "")
    except Exception:
        return ""

def md5(text: str) -> str:
    return hashlib.md5(text.encode()).hexdigest()

# ── Startup ───────────────────────────────────────────────────────────────────

@app.on_event("startup")
async def startup():
    # Install browsers on first start
    import subprocess
    logger.info("Installing Playwright browsers...")
    subprocess.run(
        ["playwright", "install", "chromium"],
        check=True
    )
    logger.info("Playwright ready")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001, loop="asyncio")