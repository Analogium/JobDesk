package com.jobdesk.web;

import com.jobdesk.service.ScraperService;
import com.jobdesk.web.dto.ScrapeResult;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Map;

/**
 * Import d'une offre par URL. Portage du {@code ScrapeController} Symfony.
 * Authentifié (couvert par la règle /api/** dans SecurityConfig).
 */
@RestController
public class ScrapeController {

    private final ScraperService scraper;

    public ScrapeController(ScraperService scraper) {
        this.scraper = scraper;
    }

    @PostMapping("/api/scrape")
    public ScrapeResult scrape(@RequestBody Map<String, String> body) {
        String url = body.get("url");
        if (url == null || url.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing or invalid \"url\" field.");
        }
        if (!isValidHttpUrl(url)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid URL format.");
        }
        try {
            return scraper.scrape(url);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RuntimeException e) {
            throw ScraperService.asBadGateway(e);
        }
    }

    private boolean isValidHttpUrl(String url) {
        try {
            URI uri = URI.create(url.replaceAll("[^\\x20-\\x7E]",
                    java.net.URLEncoder.encode("_", java.nio.charset.StandardCharsets.UTF_8)));
            String scheme = uri.getScheme();
            return uri.getHost() != null && ("http".equals(scheme) || "https".equals(scheme));
        } catch (Exception e) {
            return false;
        }
    }
}
