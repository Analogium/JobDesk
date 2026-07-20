package com.jobdesk.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobdesk.web.dto.ScrapeResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

/**
 * Scrape une offre d'emploi et renvoie des champs pré-remplis.
 * Portage de l'ancien {@code App\Service\ScraperService} (DomCrawler → JSoup).
 *
 * <p>Stratégie : fetch HTTP direct, puis repli sur le microservice Playwright
 * (sites anti-bot / SPA) si le direct échoue ou ne renvoie pas de titre.
 */
@Service
public class ScraperService {

    private static final Logger log = LoggerFactory.getLogger(ScraperService.class);

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final RestClient rest = RestClient.create();
    private final String playwrightUrl;

    public ScraperService(@Value("${app.playwright-url:}") String playwrightUrl) {
        this.playwrightUrl = (playwrightUrl == null || playwrightUrl.isBlank()) ? null : playwrightUrl;
    }

    public ScrapeResult scrape(String rawUrl) {
        String url = normalizeUrl(rawUrl);

        RuntimeException directError = null;
        ScrapeResult directResult = null;
        try {
            String html = fetchHtml(url);
            directResult = parse(html, url);
            if (directResult.jobTitle() != null) {
                return directResult;
            }
        } catch (RuntimeException e) {
            directError = e;
        }

        if (playwrightUrl != null) {
            String html = fetchHtmlWithPlaywright(url);
            return parse(html, url);
        }

        if (directError != null) {
            throw directError;
        }
        return directResult;
    }

    // ─── Routage des parsers ────────────────────────────────────────────────────

    ScrapeResult parse(String html, String url) {
        String host = hostOf(url);
        if (host.contains("welcometothejungle.com")) {
            return parseWelcomeToTheJungle(html);
        }
        if (host.contains("indeed.")) {
            return parseIndeed(html);
        }
        if (host.contains("linkedin.com")) {
            return parseLinkedin(html);
        }
        return parseFallback(html, detectSource(host));
    }

    private String detectSource(String host) {
        if (host.contains("linkedin.com")) {
            return "linkedin";
        }
        if (host.contains("indeed.")) {
            return "indeed";
        }
        if (host.contains("welcometothejungle.com")) {
            return "wttj";
        }
        return "other";
    }

    private String normalizeUrl(String url) {
        String encoded = encodeNonAscii(url);
        URI parsed;
        try {
            parsed = URI.create(encoded);
        } catch (Exception e) {
            return url;
        }
        String host = parsed.getHost() == null ? "" : parsed.getHost().toLowerCase();
        Map<String, String> query = parseQuery(parsed.getRawQuery());

        if (host.contains("linkedin.com")) {
            String jobId = query.get("currentJobId");
            if (jobId != null && jobId.matches("\\d+")) {
                return "https://www.linkedin.com/jobs/view/" + jobId;
            }
        }
        if (host.contains("indeed.")) {
            String vjk = query.get("vjk");
            if (vjk != null && vjk.matches("[0-9a-fA-F]+")) {
                return String.format("https://%s/viewjob?jk=%s", host, vjk);
            }
        }
        return url;
    }

    // ─── Parsers ────────────────────────────────────────────────────────────────

    private ScrapeResult parseWelcomeToTheJungle(String html) {
        JsonNode ld = extractJobPostingJsonLd(html);
        if (ld != null) {
            return buildFromJsonLd(ld, "wttj");
        }
        Document doc = Jsoup.parse(html);
        String jobTitle = text(doc, "h1[data-testid=job-header-title]", "h2.job-title", "h1");
        String company = text(doc, "a[data-testid=company-name]", "[data-testid=job-header-company-name]");
        String location = text(doc, "[data-testid=job-header-location] span", "[data-testid=job-header-location]");
        String contract = normalizeContractType(text(doc,
                "[data-testid=job-contract-type] span", "span[data-testid=job-contract-type]"));
        String salary = text(doc, "[data-testid=job-salary] span", "span[data-testid=job-salary]");
        String desc = descriptionText(doc, "[data-testid=job-section-description]");

        if (jobTitle == null && company == null) {
            return parseFallback(html, "wttj");
        }
        return buildResult(company, jobTitle, location, contract, salary, desc, "wttj");
    }

    private ScrapeResult parseIndeed(String html) {
        Document doc = Jsoup.parse(html);
        String jobTitle = text(doc, "h1.jobsearch-JobInfoHeader-title",
                "h1[data-testid=simpler-jobTitle]", "h1");
        String company = text(doc, "[data-testid=inlineHeader-companyName] a",
                "[data-testid=inlineHeader-companyName]",
                ".jobsearch-CompanyInfoWithoutHeaderImage a", "div[data-company-name=true]");
        String location = text(doc, "[data-testid=job-location]",
                "[data-testid=inlineHeader-companyLocation]");
        String contract = normalizeContractType(text(doc, "[data-testid=job-type-label]",
                "[role=group][aria-label*=Type] span", "[role=group][aria-label*=emploi] span",
                ".jobsearch-JobMetadataHeader-item"));
        String salary = text(doc, "[data-testid=job-salary-info]", "[data-testid=job-salary] span");
        String desc = descriptionText(doc, "#jobDescriptionText", ".jobsearch-jobDescriptionText");

        if (jobTitle == null && company == null) {
            return parseFallback(html, "indeed");
        }
        return buildResult(company, jobTitle, location, contract, salary, desc, "indeed");
    }

    private ScrapeResult parseLinkedin(String html) {
        Document doc = Jsoup.parse(html);
        String jobTitle = text(doc, "h1.top-card-layout__title",
                "h1.jobs-unified-top-card__job-title", "h1");
        String company = text(doc, "a.topcard__org-name-link",
                ".jobs-unified-top-card__company-name a");
        String location = text(doc, "span.topcard__flavor--bullet", ".jobs-unified-top-card__bullet");
        String contract = normalizeContractType(linkedinCriteriaValue(doc,
                "Type d’emploi", "Type d'emploi", "Employment type"));
        String desc = descriptionText(doc, ".show-more-less-html__markup",
                ".jobs-description-content__text", ".description__text");

        if (jobTitle == null && company == null) {
            return parseFallback(html, "linkedin");
        }
        return buildResult(company, jobTitle, location, contract, null, desc, "linkedin");
    }

    private ScrapeResult parseFallback(String html, String source) {
        JsonNode ld = extractJobPostingJsonLd(html);
        if (ld != null && jsonText(ld, "title") != null) {
            return buildFromJsonLd(ld, source);
        }
        Document doc = Jsoup.parse(html);
        String jobTitle = firstNonNull(
                metaContent(doc, "og:title"), metaContent(doc, "twitter:title"), text(doc, "h1"));
        String company = metaContent(doc, "og:site_name");
        String desc = firstNonNull(metaContent(doc, "og:description"),
                metaContent(doc, "description"), metaContent(doc, "twitter:description"));
        return buildResult(company, jobTitle, null, null, null, desc, source);
    }

    // ─── Fetch ──────────────────────────────────────────────────────────────────

    private String fetchHtml(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept-Language", "fr-FR,fr;q=0.9,en;q=0.8")
                    .GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            String html = response.body();

            if (isAntiBotBlocked(html) || status == 403 || status == 429 || status == 503) {
                throw new RuntimeException("Ce site bloque les accès automatisés. "
                        + "Copiez l'URL de l'offre directe ou saisissez les champs manuellement.");
            }
            if (status >= 400) {
                throw new RuntimeException("Le site a retourné une erreur HTTP " + status + ".");
            }
            return html;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to fetch URL: " + e.getMessage(), e);
        }
    }

    private boolean isAntiBotBlocked(String html) {
        return html.contains("Attention Required! | Cloudflare")
                || html.contains("cf-browser-verification")
                || html.contains("cf_chl_opt")
                || html.contains("window._cf_chl_opt")
                || (html.contains("Just a moment") && html.contains("cloudflare"))
                || (html.contains("You have been blocked") && html.contains("Cloudflare"))
                || html.contains("captcha-delivery.com")
                || html.contains("geo.captcha-delivery.com");
    }

    private String fetchHtmlWithPlaywright(String url) {
        try {
            JsonNode body = rest.post()
                    .uri(playwrightUrl + "/render")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("url", url))
                    .retrieve()
                    .onStatus(status -> true, (req, res) -> { })
                    .body(JsonNode.class);

            if (body == null) {
                throw new RuntimeException("Le service de rendu n'a pas répondu.");
            }
            if (body.has("error")) {
                throw new RuntimeException("Le service de rendu a échoué : " + body.get("error").asText());
            }
            if (!body.has("html") || !body.get("html").isTextual()) {
                throw new RuntimeException("Le service de rendu n'a pas retourné de HTML.");
            }
            String html = body.get("html").asText();
            if (isAntiBotBlocked(html)) {
                throw new RuntimeException("Ce site utilise une protection anti-bot avancée "
                        + "(Cloudflare/DataDome) qui bloque même le navigateur headless. "
                        + "Saisissez les informations manuellement.");
            }
            return html;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Impossible de joindre le service de rendu : " + e.getMessage(), e);
        }
    }

    // ─── Helpers JSoup ──────────────────────────────────────────────────────────

    private String text(Document doc, String... selectors) {
        for (String selector : selectors) {
            try {
                Element node = doc.selectFirst(selector);
                if (node != null) {
                    String t = node.text().trim();
                    if (!t.isEmpty()) {
                        return t;
                    }
                }
            } catch (Exception ignored) {
                // sélecteur invalide — on passe
            }
        }
        return null;
    }

    private String descriptionText(Document doc, String... selectors) {
        for (String selector : selectors) {
            try {
                Element node = doc.selectFirst(selector);
                if (node != null) {
                    String t = cleanDescription(htmlToText(node.html()));
                    if (!t.isEmpty()) {
                        return t.length() > 5000 ? t.substring(0, 5000) : t;
                    }
                }
            } catch (Exception ignored) {
                // skip
            }
        }
        return null;
    }

    private String linkedinCriteriaValue(Document doc, String... labels) {
        try {
            for (Element item : doc.select("li.description__job-criteria-item")) {
                Element h3 = item.selectFirst("h3");
                if (h3 == null) {
                    continue;
                }
                String header = h3.text().trim();
                for (String label : labels) {
                    if (header.contains(label)) {
                        Element span = item.selectFirst("span.description__job-criteria-text");
                        return span != null ? span.text().trim() : null;
                    }
                }
            }
        } catch (Exception ignored) {
            // skip
        }
        return null;
    }

    private String metaContent(Document doc, String name) {
        for (String attr : List.of("name", "property")) {
            try {
                Element node = doc.selectFirst("meta[" + attr + "=" + name + "]");
                if (node != null) {
                    String content = node.attr("content").trim();
                    if (!content.isEmpty()) {
                        return content;
                    }
                }
            } catch (Exception ignored) {
                // skip
            }
        }
        return null;
    }

    // ─── JSON-LD ────────────────────────────────────────────────────────────────

    /** Renvoie le premier bloc JobPosting valide (JSON-LD), ou null. */
    private JsonNode extractJobPostingJsonLd(String html) {
        try {
            Document doc = Jsoup.parse(html);
            for (Element script : doc.select("script[type=application/ld+json]")) {
                JsonNode node = tryParseJson(script.data());
                if (node == null) {
                    continue;
                }
                // Peut être un objet, un @graph, ou un tableau
                JsonNode posting = findJobPosting(node);
                if (posting != null) {
                    return posting;
                }
            }
        } catch (Exception e) {
            log.debug("JSON-LD parse échoué: {}", e.getMessage());
        }
        return null;
    }

    private JsonNode findJobPosting(JsonNode node) {
        if (node.isArray()) {
            for (JsonNode el : node) {
                JsonNode found = findJobPosting(el);
                if (found != null) {
                    return found;
                }
            }
        } else if (node.isObject()) {
            JsonNode type = node.get("@type");
            if (type != null && "JobPosting".equals(type.asText())) {
                return node;
            }
            if (node.has("@graph")) {
                return findJobPosting(node.get("@graph"));
            }
        }
        return null;
    }

    private ScrapeResult buildFromJsonLd(JsonNode ld, String source) {
        String company = null;
        JsonNode org = ld.path("hiringOrganization").path("name");
        if (org.isTextual()) {
            company = decodeHtml(org.asText());
        }
        String jobTitle = jsonText(ld, "title");
        if (jobTitle != null) {
            jobTitle = jobTitle.replaceAll("\\s*\\([^)]*\\)\\s*$", "").trim();
        }
        String location = null;
        JsonNode jobLocation = ld.get("jobLocation");
        if (jobLocation != null) {
            JsonNode first = jobLocation.isArray() ? jobLocation.get(0) : jobLocation;
            if (first != null) {
                String locality = first.path("address").path("addressLocality").asText(null);
                if (locality != null && !locality.isEmpty()) {
                    location = locality;
                }
            }
        }
        String contract = normalizeContractType(jsonText(ld, "employmentType"));
        String salary = extractSalary(ld.get("baseSalary"));
        String desc = null;
        String rawDesc = jsonText(ld, "description");
        if (rawDesc != null) {
            String t = cleanDescription(htmlToText(rawDesc));
            desc = t.isEmpty() ? null : (t.length() > 5000 ? t.substring(0, 5000) : t);
        }
        return buildResult(company, jobTitle, location, contract, salary, desc, source);
    }

    private String extractSalary(JsonNode baseSalary) {
        if (baseSalary == null) {
            return null;
        }
        JsonNode value = baseSalary.path("value");
        Double min = value.has("minValue") ? value.get("minValue").asDouble()
                : (value.has("value") ? value.get("value").asDouble() : null);
        Double max = value.has("maxValue") ? value.get("maxValue").asDouble() : null;
        String currency = baseSalary.path("currency").asText("€");
        if (min != null && max != null) {
            return String.format("%d-%d %s", min.intValue(), max.intValue(), currency);
        }
        if (min != null) {
            return String.format("%d %s", min.intValue(), currency);
        }
        return null;
    }

    // ─── Normalisation ──────────────────────────────────────────────────────────

    String normalizeContractType(String raw) {
        if (raw == null) {
            return null;
        }
        Map<String, String> schemaMap = Map.of(
                "FULL_TIME", "CDI", "PART_TIME", "CDI", "CONTRACTOR", "FREELANCE",
                "TEMPORARY", "CDD", "INTERN", "INTERNSHIP");
        if (schemaMap.containsKey(raw)) {
            return schemaMap.get(raw);
        }
        String lower = raw.toLowerCase();
        if (lower.contains("cdi") || lower.contains("permanent") || lower.contains("full-time")
                || lower.contains("temps plein")) {
            return "CDI";
        }
        if (lower.contains("cdd") || lower.contains("temporary")) {
            return "CDD";
        }
        if (lower.contains("alternance") || lower.contains("apprenti")) {
            return "ALTERNANCE";
        }
        if (lower.contains("stage") || lower.contains("intern")) {
            return "INTERNSHIP";
        }
        if (lower.contains("freelance") || lower.contains("independant") || lower.equals("contrat")) {
            return "FREELANCE";
        }
        return null;
    }

    private String htmlToText(String html) {
        // Insère un \n avant les fermetures de blocs, puis retire les balises et décode les entités.
        String text = html.replaceAll("(?i)</(p|div|li|h[1-6]|section|article)>", "\n");
        text = text.replaceAll("(?i)<br\\s*/?>", "\n");
        text = text.replaceAll("<[^>]+>", "");
        return org.jsoup.parser.Parser.unescapeEntities(text, false);
    }

    private String cleanDescription(String text) {
        String t = text.replaceAll("(?iu)[ \\t]*(Show more|Show less|Voir plus|Voir moins)[ \\t]*", "");
        t = t.replaceAll("[ \\t]+", " ");
        t = t.replaceAll("\\n{3,}", "\n\n");
        return t.trim();
    }

    private ScrapeResult buildResult(String company, String jobTitle, String location,
                                     String contract, String salary, String desc, String source) {
        return new ScrapeResult(
                emptyToNull(company), emptyToNull(jobTitle), emptyToNull(location),
                contract, emptyToNull(salary), emptyToNull(desc), source);
    }

    // ─── Utils ──────────────────────────────────────────────────────────────────

    private static final Pattern NON_ASCII = Pattern.compile("[^\\x20-\\x7E]");

    private String encodeNonAscii(String url) {
        Matcher m = NON_ASCII.matcher(url);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(
                    URLEncoder.encode(m.group(), StandardCharsets.UTF_8)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> map = new java.util.HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return map;
        }
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                map.put(pair.substring(0, eq), pair.substring(eq + 1));
            }
        }
        return map;
    }

    private String hostOf(String url) {
        try {
            String host = URI.create(encodeNonAscii(url)).getHost();
            return host == null ? "" : host.toLowerCase();
        } catch (Exception e) {
            return "";
        }
    }

    private JsonNode tryParseJson(String raw) {
        try {
            return mapper.readTree(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private String jsonText(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v != null && v.isTextual() && !v.asText().isEmpty()) ? v.asText() : null;
    }

    private String decodeHtml(String s) {
        return org.jsoup.parser.Parser.unescapeEntities(s, false);
    }

    private String firstNonNull(String... values) {
        for (String v : values) {
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private String emptyToNull(String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }

    /** Traduit une RuntimeException de scraping en réponse HTTP 502. */
    public static ResponseStatusException asBadGateway(RuntimeException e) {
        return new ResponseStatusException(BAD_GATEWAY, e.getMessage());
    }
}
