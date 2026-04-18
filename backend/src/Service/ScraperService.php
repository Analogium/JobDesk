<?php

namespace App\Service;

use Exception;
use JsonException;
use RuntimeException;
use Symfony\Component\DomCrawler\Crawler;
use Symfony\Contracts\HttpClient\Exception\TransportExceptionInterface;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class ScraperService
{
    private const USER_AGENT = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36';

    private readonly ?string $playwrightUrl;

    public function __construct(
        private readonly HttpClientInterface $httpClient,
        string $playwrightUrl = '',
    ) {
        $this->playwrightUrl = '' !== $playwrightUrl ? $playwrightUrl : null;
    }

    /**
     * Scrape a job offer URL and return pre-filled form fields.
     *
     * Strategy:
     *   1. Try a direct HTTP fetch (fast, no overhead).
     *   2. If the site blocks server-side requests (Cloudflare, 403…) OR the
     *      result has no jobTitle (SPA with client-side rendering), retry using
     *      the Playwright headless-browser service.
     *   3. If Playwright is not configured, re-throw the error or return the
     *      partial result from step 1.
     *
     * @return array{
     *   companyName: string|null,
     *   jobTitle: string|null,
     *   location: string|null,
     *   contractType: string|null,
     *   salaryRange: string|null,
     *   jobDescription: string|null,
     *   source: string
     * }
     */
    public function scrape(string $url): array
    {
        $url = $this->normalizeUrl($url);

        $directError = null;
        $directResult = null;

        try {
            $html = $this->fetchHtml($url);
            $directResult = $this->parse($html, $url);

            // Direct fetch succeeded and we got meaningful data — done.
            if (null !== $directResult['jobTitle']) {
                return $directResult;
            }
        } catch (RuntimeException $e) {
            $directError = $e;
        }

        // Try Playwright if configured (handles anti-bot walls and SPAs).
        if (null !== $this->playwrightUrl) {
            $html = $this->fetchHtmlWithPlaywright($url);

            return $this->parse($html, $url);
        }

        // No Playwright available: surface the error or the empty result.
        if (null !== $directError) {
            throw $directError;
        }

        return $directResult; // partial (no jobTitle) but no error
    }

    /** Route HTML to the right parser based on the URL host. */
    private function parse(string $html, string $url): array
    {
        $host = strtolower(parse_url($url, PHP_URL_HOST) ?? '');

        return match (true) {
            str_contains($host, 'welcometothejungle.com') => $this->parseWelcomeToTheJungle($html),
            str_contains($host, 'indeed.') => $this->parseIndeed($html),
            str_contains($host, 'linkedin.com') => $this->parseLinkedin($html),
            default => $this->parseFallback($html, $this->detectSource($host)),
        };
    }

    private function detectSource(string $host): string
    {
        return match (true) {
            str_contains($host, 'linkedin.com') => 'linkedin',
            str_contains($host, 'indeed.') => 'indeed',
            str_contains($host, 'welcometothejungle.com') => 'wttj',
            default => 'other',
        };
    }

    /**
     * Normalize known URL patterns to canonical job-detail URLs.
     * e.g. LinkedIn search URLs with currentJobId → /jobs/view/{id}.
     */
    private function normalizeUrl(string $url): string
    {
        // Encode non-ASCII chars so parse_url works reliably
        $url = preg_replace_callback('/[^\x20-\x7E]/', static fn ($m) => rawurlencode($m[0]), $url) ?? $url;

        $parsed = parse_url($url);
        $host = strtolower($parsed['host'] ?? '');

        if (str_contains($host, 'linkedin.com')) {
            parse_str($parsed['query'] ?? '', $query);
            if (!empty($query['currentJobId']) && preg_match('/^\d+$/', $query['currentJobId'])) {
                return 'https://www.linkedin.com/jobs/view/'.$query['currentJobId'];
            }
        }

        if (str_contains($host, 'indeed.')) {
            parse_str($parsed['query'] ?? '', $query);
            if (!empty($query['vjk']) && preg_match('/^[0-9a-f]+$/i', $query['vjk'])) {
                // Keep the subdomain (fr.indeed.com, indeed.com, etc.)
                return sprintf('https://%s/viewjob?jk=%s', $host, $query['vjk']);
            }
        }

        return $url;
    }

    // ─── Parsers ──────────────────────────────────────────────────────────────

    private function parseWelcomeToTheJungle(string $html): array
    {
        // Prefer JSON-LD JobPosting — structured, stable, unaffected by CSS class changes.
        $ld = $this->extractJobPostingJsonLd($html);
        if (null !== $ld) {
            return $this->buildResult(
                $ld['companyName'],
                $ld['jobTitle'],
                $ld['location'],
                $this->normalizeContractType($ld['employmentType']),
                $ld['salaryRange'],
                $ld['jobDescription'],
                'wttj',
            );
        }

        // Fallback: CSS selectors (obfuscated class names — less stable).
        $crawler = new Crawler($html);

        $jobTitle = $this->text($crawler, [
            'h1[data-testid="job-header-title"]',
            'h2.job-title',
            'h1',
        ]);

        $companyName = $this->text($crawler, [
            'a[data-testid="company-name"]',
            '[data-testid="job-header-company-name"]',
        ]);

        $location = $this->text($crawler, [
            '[data-testid="job-header-location"] span',
            '[data-testid="job-header-location"]',
        ]);

        $contractType = $this->normalizeContractType($this->text($crawler, [
            '[data-testid="job-contract-type"] span',
            'span[data-testid="job-contract-type"]',
        ]));

        $salaryRange = $this->text($crawler, [
            '[data-testid="job-salary"] span',
            'span[data-testid="job-salary"]',
        ]);

        $jobDescription = $this->descriptionText($crawler, [
            '[data-testid="job-section-description"]',
        ]);

        if (null === $jobTitle && null === $companyName) {
            return $this->parseFallback($html, 'wttj');
        }

        return $this->buildResult($companyName, $jobTitle, $location, $contractType, $salaryRange, $jobDescription, 'wttj');
    }

    private function parseIndeed(string $html): array
    {
        $crawler = new Crawler($html);

        $jobTitle = $this->text($crawler, [
            'h1.jobsearch-JobInfoHeader-title',
            'h1[data-testid="simpler-jobTitle"]',
            'h1',
        ]);

        $companyName = $this->text($crawler, [
            '[data-testid="inlineHeader-companyName"] a',
            '[data-testid="inlineHeader-companyName"]',
            '.jobsearch-CompanyInfoWithoutHeaderImage a',
            'div[data-company-name="true"]',
        ]);

        $location = $this->text($crawler, [
            '[data-testid="job-location"]',
            '[data-testid="inlineHeader-companyLocation"]',
        ]);

        $contractType = $this->normalizeContractType($this->text($crawler, [
            '[data-testid="job-type-label"]',
            // Indeed renders contract type inside a role="group" aria-label="Type d'emploi"
            '[role="group"][aria-label*="Type"] span',
            '[role="group"][aria-label*="emploi"] span',
            '.jobsearch-JobMetadataHeader-item',
        ]));

        $salaryRange = $this->text($crawler, [
            '[data-testid="job-salary-info"]',
            '[data-testid="job-salary"] span',
        ]);

        $jobDescription = $this->descriptionText($crawler, [
            '#jobDescriptionText',
            '.jobsearch-jobDescriptionText',
        ]);

        if (null === $jobTitle && null === $companyName) {
            return $this->parseFallback($html, 'indeed');
        }

        return $this->buildResult($companyName, $jobTitle, $location, $contractType, $salaryRange, $jobDescription, 'indeed');
    }

    private function parseLinkedin(string $html): array
    {
        $crawler = new Crawler($html);

        $jobTitle = $this->text($crawler, [
            'h1.top-card-layout__title',
            'h1.jobs-unified-top-card__job-title',
            'h1',
        ]);

        $companyName = $this->text($crawler, [
            'a.topcard__org-name-link',
            '.jobs-unified-top-card__company-name a',
        ]);

        $location = $this->text($crawler, [
            'span.topcard__flavor--bullet',
            '.jobs-unified-top-card__bullet',
        ]);

        // Extract contract type from the criteria list: find the <li> whose <h3>
        // says "Type d'emploi" / "Employment type", then read its <span>.
        $contractType = $this->normalizeContractType($this->linkedinCriteriaValue($crawler, [
            "Type d\u{2019}emploi", "Type d'emploi", 'Employment type',
        ]));

        $jobDescription = $this->descriptionText($crawler, [
            '.show-more-less-html__markup',
            '.jobs-description-content__text',
            '.description__text',
        ]);

        if (null === $jobTitle && null === $companyName) {
            return $this->parseFallback($html, 'linkedin');
        }

        return $this->buildResult($companyName, $jobTitle, $location, $contractType, null, $jobDescription, 'linkedin');
    }

    private function parseFallback(string $html, string $source = 'other'): array
    {
        // Try structured JSON-LD first (Himalayas, Wellfound, Collective.work, etc.)
        $ld = $this->extractJobPostingJsonLd($html);
        if (null !== $ld && null !== $ld['jobTitle']) {
            return $this->buildResult(
                $ld['companyName'],
                $ld['jobTitle'],
                $ld['location'],
                $this->normalizeContractType($ld['employmentType']),
                $ld['salaryRange'],
                $ld['jobDescription'],
                $source,
            );
        }

        // Fallback: Open Graph / meta tags
        $crawler = new Crawler($html);

        $jobTitle = $this->metaContent($crawler, 'og:title')
            ?? $this->metaContent($crawler, 'twitter:title')
            ?? $this->text($crawler, ['h1']);

        $companyName = $this->metaContent($crawler, 'og:site_name');

        $description = $this->metaContent($crawler, 'og:description')
            ?? $this->metaContent($crawler, 'description')
            ?? $this->metaContent($crawler, 'twitter:description');

        return $this->buildResult($companyName, $jobTitle, null, null, null, $description, $source);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private function fetchHtml(string $url): string
    {
        try {
            $response = $this->httpClient->request('GET', $url, [
                'headers' => [
                    'User-Agent' => self::USER_AGENT,
                    'Accept-Language' => 'fr-FR,fr;q=0.9,en;q=0.8',
                ],
                'timeout' => 10,
                'max_redirects' => 5,
            ]);

            $status = $response->getStatusCode();
            // getContent(false) never throws on HTTP errors, letting us inspect the body first
            $html = $response->getContent(false);

            if ($this->isAntiBotBlocked($html) || \in_array($status, [403, 429, 503], true)) {
                throw new RuntimeException('Ce site bloque les accès automatisés. Copiez l\'URL de l\'offre directe ou saisissez les champs manuellement.');
            }

            if ($status >= 400) {
                throw new RuntimeException(sprintf('Le site a retourné une erreur HTTP %d.', $status));
            }

            return $html;
        } catch (TransportExceptionInterface $e) {
            throw new RuntimeException(sprintf('Unable to fetch URL: %s', $e->getMessage()), 0, $e);
        }
    }

    /** Detect Cloudflare, DataDome, or other anti-bot challenge pages. */
    private function isAntiBotBlocked(string $html): bool
    {
        return str_contains($html, 'Attention Required! | Cloudflare')
            || str_contains($html, 'cf-browser-verification')
            || str_contains($html, 'cf_chl_opt')
            || str_contains($html, 'window._cf_chl_opt')
            // Cloudflare JS challenge ("Just a moment...")
            || (str_contains($html, 'Just a moment') && str_contains($html, 'cloudflare'))
            || (str_contains($html, 'You have been blocked') && str_contains($html, 'Cloudflare'))
            // DataDome CAPTCHA
            || str_contains($html, 'captcha-delivery.com')
            || str_contains($html, 'geo.captcha-delivery.com');
    }

    /**
     * Fetch rendered HTML via the Playwright headless-browser microservice.
     * Used for sites that block direct HTTP requests (Cloudflare, SPAs, etc.).
     *
     * @throws RuntimeException on network error or Playwright service error
     */
    private function fetchHtmlWithPlaywright(string $url): string
    {
        try {
            $response = $this->httpClient->request('POST', $this->playwrightUrl.'/render', [
                'headers' => ['Content-Type' => 'application/json'],
                'json' => ['url' => $url],
                'timeout' => 45,
            ]);

            $status = $response->getStatusCode();
            $body = $response->toArray(false);

            if ($status >= 400 || isset($body['error'])) {
                $msg = $body['error'] ?? sprintf('Playwright service returned HTTP %d', $status);
                throw new RuntimeException(sprintf('Le service de rendu a échoué : %s', $msg));
            }

            if (!isset($body['html']) || !is_string($body['html'])) {
                throw new RuntimeException('Le service de rendu n\'a pas retourné de HTML.');
            }

            $html = $body['html'];

            if ($this->isAntiBotBlocked($html)) {
                throw new RuntimeException('Ce site utilise une protection anti-bot avancée (Cloudflare/DataDome) qui bloque même le navigateur headless. Saisissez les informations manuellement.');
            }

            return $html;
        } catch (TransportExceptionInterface $e) {
            throw new RuntimeException(sprintf('Impossible de joindre le service de rendu : %s', $e->getMessage()), 0, $e);
        }
    }

    /** Try each CSS selector in order, return the first non-empty trimmed text. */
    private function text(Crawler $crawler, array $selectors): ?string
    {
        foreach ($selectors as $selector) {
            try {
                $node = $crawler->filter($selector);
                if ($node->count() > 0) {
                    $text = trim($node->first()->text('', false));
                    if ('' !== $text) {
                        return $text;
                    }
                }
            } catch (Exception) {
                // invalid selector — skip
            }
        }

        return null;
    }

    /** Extract description as plain text from a container, with whitespace normalization. */
    private function descriptionText(Crawler $crawler, array $selectors): ?string
    {
        foreach ($selectors as $selector) {
            try {
                $node = $crawler->filter($selector);
                if ($node->count() > 0) {
                    $text = $this->htmlToText($node->first()->html(''));
                    $text = $this->cleanDescription($text);
                    if ('' !== $text) {
                        return mb_substr($text, 0, 5000);
                    }
                }
            } catch (Exception) {
                // invalid selector — skip
            }
        }

        return null;
    }

    /**
     * Convert an HTML fragment to plain text, inserting newlines at block boundaries
     * so that paragraphs and list items are human-readable.
     */
    private function htmlToText(string $html): string
    {
        // Insert newline before closing block tags so content is separated
        $text = preg_replace('/<\/(p|div|li|h[1-6]|section|article)>/i', "\n", $html) ?? $html;
        $text = preg_replace('/<br\s*\/?>/i', "\n", $text) ?? $text;
        $text = strip_tags($text);
        // Decode HTML entities
        $text = html_entity_decode($text, ENT_QUOTES | ENT_HTML5, 'UTF-8');

        return $text;
    }

    /** Normalize whitespace and strip UI artefacts from scraped descriptions. */
    private function cleanDescription(string $text): string
    {
        // Remove "Show more" / "Show less" button labels (LinkedIn, WTTJ…)
        $text = preg_replace('/[ \t]*(Show more|Show less|Voir plus|Voir moins)[ \t]*/ui', '', $text) ?? $text;
        // Collapse runs of spaces/tabs (but not newlines)
        $text = preg_replace('/[ \t]+/', ' ', $text) ?? $text;
        // Collapse 3+ consecutive newlines to 2
        $text = preg_replace('/\n{3,}/', "\n\n", $text) ?? $text;

        return trim($text);
    }

    /**
     * LinkedIn-specific: find a criteria value by its label.
     * Searches <li.description__job-criteria-item> elements for an <h3> matching
     * one of the given labels, then returns the associated <span> text.
     *
     * @param string[] $labels
     */
    private function linkedinCriteriaValue(Crawler $crawler, array $labels): ?string
    {
        try {
            $items = $crawler->filter('li.description__job-criteria-item');
            foreach ($items as $item) {
                $li = new Crawler($item);
                $header = trim($li->filter('h3')->text('', true));
                foreach ($labels as $label) {
                    if (str_contains($header, $label)) {
                        return trim($li->filter('span.description__job-criteria-text')->text('', true));
                    }
                }
            }
        } catch (Exception) {
            // skip
        }

        return null;
    }

    private function metaContent(Crawler $crawler, string $name): ?string
    {
        foreach (['name', 'property'] as $attr) {
            try {
                $node = $crawler->filter(sprintf('meta[%s="%s"]', $attr, $name));
                if ($node->count() > 0) {
                    $content = trim($node->attr('content') ?? '');
                    if ('' !== $content) {
                        return $content;
                    }
                }
            } catch (Exception) {
                // skip
            }
        }

        return null;
    }

    /**
     * Extract a JobPosting from the first <script type="application/ld+json"> block found.
     * Returns a normalized field map, or null if no valid JobPosting is present.
     *
     * @return array{companyName: string|null, jobTitle: string|null, location: string|null, employmentType: string|null, salaryRange: string|null, jobDescription: string|null}|null
     */
    private function extractJobPostingJsonLd(string $html): ?array
    {
        if (!preg_match_all('/<script[^>]+type="application\/ld\+json"[^>]*>(.*?)<\/script>/si', $html, $matches)) {
            return null;
        }

        foreach ($matches[1] as $raw) {
            try {
                $data = json_decode($raw, true, 512, JSON_THROW_ON_ERROR);
            } catch (JsonException) {
                continue;
            }

            if (!is_array($data) || ($data['@type'] ?? '') !== 'JobPosting') {
                continue;
            }

            // Company name
            $companyName = null;
            if (isset($data['hiringOrganization']['name'])) {
                $companyName = html_entity_decode((string) $data['hiringOrganization']['name'], ENT_QUOTES | ENT_HTML5, 'UTF-8');
            }

            // Job title — strip parenthesised contract type suffixes like "(CDI)"
            $jobTitle = null;
            if (isset($data['title'])) {
                $jobTitle = trim(preg_replace('/\s*\([^)]*\)\s*$/', '', (string) $data['title']) ?? $data['title']);
            }

            // Location: first jobLocation entry
            $location = null;
            $jobLocations = $data['jobLocation'] ?? null;
            if (is_array($jobLocations)) {
                $first = is_array($jobLocations[0] ?? null) ? $jobLocations[0] : $jobLocations;
                $locality = $first['address']['addressLocality'] ?? null;
                if (is_string($locality) && '' !== $locality) {
                    $location = $locality;
                }
            }

            // Employment type (schema.org enum: FULL_TIME, PART_TIME, CONTRACTOR, TEMPORARY, INTERN, OTHER)
            $employmentType = is_string($data['employmentType'] ?? null) ? $data['employmentType'] : null;

            // Salary
            $salaryRange = null;
            if (isset($data['baseSalary'])) {
                $sal = $data['baseSalary'];
                $min = $sal['value']['minValue'] ?? $sal['value']['value'] ?? null;
                $max = $sal['value']['maxValue'] ?? null;
                $currency = $sal['currency'] ?? '€';
                if (null !== $min && null !== $max) {
                    $salaryRange = sprintf('%s-%s %s', (int) $min, (int) $max, $currency);
                } elseif (null !== $min) {
                    $salaryRange = sprintf('%s %s', (int) $min, $currency);
                }
            }

            // Description: HTML → plain text
            $jobDescription = null;
            if (isset($data['description']) && is_string($data['description'])) {
                $text = $this->htmlToText($data['description']);
                $text = $this->cleanDescription($text);
                $jobDescription = mb_substr($text, 0, 5000) ?: null;
            }

            return compact('companyName', 'jobTitle', 'location', 'employmentType', 'salaryRange', 'jobDescription');
        }

        return null;
    }

    private function normalizeContractType(?string $raw): ?string
    {
        if (null === $raw) {
            return null;
        }

        // schema.org JobPosting employmentType enum values
        $schemaMap = [
            'FULL_TIME' => 'CDI',
            'PART_TIME' => 'CDI',
            'CONTRACTOR' => 'FREELANCE',
            'TEMPORARY' => 'CDD',
            'INTERN' => 'INTERNSHIP',
        ];
        if (isset($schemaMap[$raw])) {
            return $schemaMap[$raw];
        }

        $lower = strtolower($raw);

        return match (true) {
            str_contains($lower, 'cdi') || str_contains($lower, 'permanent') || str_contains($lower, 'full-time') || str_contains($lower, 'temps plein') => 'CDI',
            str_contains($lower, 'cdd') || str_contains($lower, 'temporary') => 'CDD',
            str_contains($lower, 'alternance') || str_contains($lower, 'apprenti') => 'ALTERNANCE',
            str_contains($lower, 'stage') || str_contains($lower, 'intern') => 'INTERNSHIP',
            // "Contrat" alone (LinkedIn FR) or "freelance" maps to FREELANCE
            str_contains($lower, 'freelance') || str_contains($lower, 'independant') || 'contrat' === $lower => 'FREELANCE',
            default => null,
        };
    }

    /**
     * @return array{companyName: string|null, jobTitle: string|null, location: string|null, contractType: string|null, salaryRange: string|null, jobDescription: string|null, source: string}
     */
    private function buildResult(
        ?string $companyName,
        ?string $jobTitle,
        ?string $location,
        ?string $contractType,
        ?string $salaryRange,
        ?string $jobDescription,
        string $source,
    ): array {
        return [
            'companyName' => $companyName ?: null,
            'jobTitle' => $jobTitle ?: null,
            'location' => $location ?: null,
            'contractType' => $contractType,
            'salaryRange' => $salaryRange ?: null,
            'jobDescription' => $jobDescription ?: null,
            'source' => $source,
        ];
    }
}
