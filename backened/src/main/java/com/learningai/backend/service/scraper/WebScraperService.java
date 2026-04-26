package com.learningai.backend.service.scraper;

import com.learningai.backend.config.ScraperConfig;
import com.learningai.backend.entity.ScrapedContent;
import com.learningai.backend.repository.ScrapedContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebScraperService {

    private final ScrapedContentRepository contentRepository;
    private final PlaywrightScraperClient playwrightClient;

    // ─── Main scrape entry point ──────────────────────────────────────────

    public Optional<ScrapedContent> scrape(String url,
            String conceptTag,
            String conceptName,
            String scrapeReason) {
        try {
            // 1. Validate URL
            if (!isValidUrl(url)) {
                log.warn("Invalid URL skipped: {}", url);
                return Optional.empty();
            }

            // 2. Dedup check
            String urlHash = md5(url);
            if (contentRepository.existsByUrlHash(urlHash)) {
                log.info("Already scraped, skipping: {}", url);
                return contentRepository.findByUrlHash(urlHash);
            }

            // 3. Rate limit
            Thread.sleep(ScraperConfig.RATE_LIMIT_DELAY_MS);

            // 4. Fetch and parse
            log.info("Scraping: {}", url);

            Document doc = Jsoup.connect(url)
                    .userAgent(ScraperConfig.USER_AGENT)
                    .timeout(ScraperConfig.REQUEST_TIMEOUT_MS)
                    .ignoreHttpErrors(false)
                    .get();

            // 5. Extract clean text
            String title = extractTitle(doc);
            String bodyText = extractBodyText(doc, url);

            String decodedTag = java.net.URLDecoder.decode(
                    conceptTag, java.nio.charset.StandardCharsets.UTF_8);
            String decodedName = java.net.URLDecoder.decode(
                    conceptName != null ? conceptName : "",
                    java.nio.charset.StandardCharsets.UTF_8);

            // 6. Validate content quality
            if (bodyText.length() < ScraperConfig.MIN_BODY_LENGTH_CHARS) {
                log.warn("Content too short ({}chars), skipping: {}",
                        bodyText.length(), url);
                Optional<PlaywrightScraperClient.PlaywrightResult> pwResult = playwrightClient.scrape(url, decodedTag,
                        decodedName);

                if (pwResult.isPresent() &&
                        pwResult.get().getBodyText().length() >= ScraperConfig.MIN_BODY_LENGTH_CHARS) {

                    title = pwResult.get().getTitle();
                    bodyText = pwResult.get().getBodyText();
                    scrapeReason = "PLAYWRIGHT_" + scrapeReason;
                    log.info("Playwright succeeded for: {} ({} chars)",
                            url, bodyText.length());
                } else {
                    log.warn("Both Jsoup and Playwright failed for: {}", url);
                    return Optional.empty();
                }
            }

            // 7. Trim if too long
            if (bodyText.length() > ScraperConfig.MAX_BODY_LENGTH_CHARS) {
                bodyText = bodyText.substring(
                        0, ScraperConfig.MAX_BODY_LENGTH_CHARS);
            }

            // 8. Build and save entity
            ScrapedContent content = ScrapedContent.builder()
                    .url(url)
                    .urlHash(urlHash)
                    .title(title)
                    .bodyText(bodyText)
                    .conceptTag(decodedTag)
                    .conceptName(decodedName)
                    .source(extractDomain(url))
                    .wordCount(countWords(bodyText))
                    .embedded(false)
                    .retrievalCount(0)
                    .scrapeReason(scrapeReason)
                    .build();

            content = contentRepository.save(content);
            log.info("Scraped and saved: {} ({} words)",
                    url, content.getWordCount());

            return Optional.of(content);
        } catch (
        InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Scraping interrupted: {}", url);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to scrape {}: {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    // ─── Scrape multiple URLs ─────────────────────────────────────────────

    public List<ScrapedContent> scrapeAll(List<String> urls,
            String conceptTag,
            String conceptName,
            String scrapeReason) {
        List<ScrapedContent> results = new ArrayList<>();
        for (String url : urls) {
            scrape(url, conceptTag, conceptName, scrapeReason)
                    .ifPresent(results::add);
        }
        log.info("Scraped {}/{} URLs for topic:{}",
                results.size(), urls.size(), conceptTag);
        return results;
    }

    // ─── Smart text extraction (site-aware) ───────────────────────────────

    private String extractBodyText(Document doc, String url) {
        String domain = extractDomain(url);

        // Remove noise elements first
        doc.select("nav, header, footer, aside, " +
                ".sidebar, .advertisement, .ads, .cookie, " +
                ".popup, .modal, script, style, " +
                ".social-share, .related-posts, " +
                "#comments, .comment-section").remove();

        // Site-specific extraction for best quality
        String text = switch (domain) {
            case "geeksforgeeks.org" ->
                extractFromSelectors(doc,
                        "article.content", ".article-body",
                        ".entry-content", "div.text");

            case "en.wikipedia.org" ->
                extractFromSelectors(doc,
                        "#mw-content-text .mw-parser-output",
                        "#bodyContent");

            case "baeldung.com" ->
                extractFromSelectors(doc,
                        ".entry-content", "article");

            case "medium.com", "dev.to",
                    "towardsdatascience.com" ->
                extractFromSelectors(doc,
                        "article", ".postArticle-content",
                        "[data-article-body]");

            case "investopedia.com" ->
                extractFromSelectors(doc,
                        ".article-body-content",
                        "[data-click-tracked]", "article");

            case "stackoverflow.com" ->
                extractFromSelectors(doc,
                        ".answercell .s-prose",
                        ".question .s-prose");

            default ->
                extractFromSelectors(doc,
                        "article", "main",
                        ".content", ".post-content",
                        ".entry-content", "#content",
                        "div.body");
        };

        // Final fallback — grab all paragraph text
        if (text.length() < ScraperConfig.MIN_BODY_LENGTH_CHARS) {
            StringBuilder sb = new StringBuilder();
            doc.select("p, h1, h2, h3, h4, li, code, pre")
                    .forEach(el -> sb.append(el.text()).append("\n"));
            text = sb.toString();
        }

        return cleanText(text);
    }

    private String extractFromSelectors(Document doc,
            String... selectors) {
        for (String selector : selectors) {
            Elements elements = doc.select(selector);
            if (!elements.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Element el : elements) {
                    // Preserve code blocks
                    el.select("pre, code").forEach(code -> code.text("[CODE: " + code.text() + "]"));
                    sb.append(el.text()).append("\n\n");
                }
                String text = cleanText(sb.toString());
                if (text.length() >= ScraperConfig.MIN_BODY_LENGTH_CHARS) {
                    return text;
                }
            }
        }
        return "";
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private String extractTitle(Document doc) {
        String title = doc.title();
        if (title == null || title.isBlank()) {
            Element h1 = doc.selectFirst("h1");
            title = h1 != null ? h1.text() : "Untitled";
        }
        // Remove site name suffix (e.g. " - GeeksforGeeks")
        return title.replaceAll("\\s*[-|]\\s*[^-|]+$", "").trim();
    }

    private String cleanText(String raw) {
        if (raw == null)
            return "";
        return raw
                // Remove HTML entities
                .replaceAll("&[a-zA-Z]+;", " ")
                // Collapse whitespace
                .replaceAll("[ \\t]+", " ")
                // Max 2 newlines in a row
                .replaceAll("\\n{3,}", "\n\n")
                // Remove lines that are just punctuation or numbers
                .replaceAll("(?m)^[^a-zA-Z]*$\\n?", "")
                .trim();
    }

    private String extractDomain(String url) {
        try {
            String host = new URI(url).getHost();
            if (host == null)
                return "unknown";
            return host.startsWith("www.")
                    ? host.substring(4)
                    : host;
        } catch (Exception e) {
            return "unknown";
        }
    }

    private boolean isValidUrl(String url) {
        if (url == null || url.isBlank())
            return false;
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                return false;
            }
            String domain = extractDomain(url);
            // Block known bad domains
            return !ScraperConfig.BLOCKED_DOMAINS.contains(domain);
        } catch (Exception e) {
            return false;
        }
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }

    private int countWords(String text) {
        if (text == null || text.isBlank())
            return 0;
        return text.trim().split("\\s+").length;
    }
}