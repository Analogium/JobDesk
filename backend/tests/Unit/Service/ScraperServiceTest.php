<?php

namespace App\Tests\Unit\Service;

use App\Service\ScraperService;
use PHPUnit\Framework\TestCase;
use RuntimeException;
use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Contracts\HttpClient\ResponseInterface;

class ScraperServiceTest extends TestCase
{
    // ─── Welcome to the Jungle ────────────────────────────────────────────────

    public function testParseWelcomeToTheJungle(): void
    {
        $html = <<<'HTML'
            <html><body>
                <h1 data-testid="job-header-title">Développeur Backend PHP</h1>
                <a data-testid="company-name">Acme Corp</a>
                <div data-testid="job-header-location"><span>Paris, France</span></div>
                <div data-testid="job-contract-type"><span>CDI</span></div>
                <div data-testid="job-salary"><span>45-55k€</span></div>
                <div data-testid="job-section-description">Description du poste.</div>
            </body></html>
        HTML;

        $result = $this->scrapeWithHtml('https://www.welcometothejungle.com/fr/companies/acme/jobs/dev', $html);

        $this->assertSame('Développeur Backend PHP', $result['jobTitle']);
        $this->assertSame('Acme Corp', $result['companyName']);
        $this->assertSame('Paris, France', $result['location']);
        $this->assertSame('CDI', $result['contractType']);
        $this->assertSame('45-55k€', $result['salaryRange']);
        $this->assertSame('Description du poste.', $result['jobDescription']);
        $this->assertSame('wttj', $result['source']);
    }

    public function testWttjFallsBackToGenericWhenSelectionEmpty(): void
    {
        $html = <<<'HTML'
            <html><head>
                <meta property="og:title" content="Ingénieur Data" />
                <meta property="og:site_name" content="Welcome to the Jungle" />
                <meta property="og:description" content="Rejoignez notre équipe data." />
            </head><body></body></html>
        HTML;

        $result = $this->scrapeWithHtml('https://www.welcometothejungle.com/fr/companies/acme/jobs/data', $html);

        $this->assertSame('Ingénieur Data', $result['jobTitle']);
        $this->assertSame('Welcome to the Jungle', $result['companyName']);
        $this->assertSame('Rejoignez notre équipe data.', $result['jobDescription']);
        $this->assertSame('wttj', $result['source']);
    }

    // ─── Indeed ───────────────────────────────────────────────────────────────

    public function testParseIndeed(): void
    {
        $html = <<<'HTML'
            <html><body>
                <h1 class="jobsearch-JobInfoHeader-title">Software Engineer</h1>
                <div data-testid="inlineHeader-companyName"><a>Tech SAS</a></div>
                <div data-testid="job-location">Lyon, 69</div>
                <div data-testid="job-type-label">CDI</div>
                <div id="jobDescriptionText">We are looking for a backend developer.</div>
            </body></html>
        HTML;

        $result = $this->scrapeWithHtml('https://fr.indeed.com/viewjob?jk=abc123', $html);

        $this->assertSame('Software Engineer', $result['jobTitle']);
        $this->assertSame('Tech SAS', $result['companyName']);
        $this->assertSame('Lyon, 69', $result['location']);
        $this->assertSame('CDI', $result['contractType']);
        $this->assertSame('We are looking for a backend developer.', $result['jobDescription']);
        $this->assertSame('indeed', $result['source']);
    }

    public function testIndeedFallsBackToGenericWhenSelectionEmpty(): void
    {
        $html = <<<'HTML'
            <html><head>
                <meta property="og:title" content="DevOps Engineer" />
                <meta property="og:site_name" content="Indeed" />
            </head><body></body></html>
        HTML;

        $result = $this->scrapeWithHtml('https://indeed.com/viewjob?jk=xyz', $html);

        $this->assertSame('DevOps Engineer', $result['jobTitle']);
        $this->assertSame('Indeed', $result['companyName']);
        $this->assertSame('indeed', $result['source']);
    }

    // ─── LinkedIn ─────────────────────────────────────────────────────────────

    public function testParseLinkedin(): void
    {
        $html = <<<'HTML'
            <html><body>
                <h1 class="top-card-layout__title">Product Manager</h1>
                <a class="topcard__org-name-link">StartupXYZ</a>
                <span class="topcard__flavor--bullet">Remote</span>
            </body></html>
        HTML;

        $result = $this->scrapeWithHtml('https://www.linkedin.com/jobs/view/123456', $html);

        $this->assertSame('Product Manager', $result['jobTitle']);
        $this->assertSame('StartupXYZ', $result['companyName']);
        $this->assertSame('Remote', $result['location']);
        $this->assertSame('linkedin', $result['source']);
    }

    public function testLinkedinFallsBackToGenericWhenSelectionEmpty(): void
    {
        $html = <<<'HTML'
            <html><head>
                <meta property="og:title" content="UX Designer" />
                <meta property="og:site_name" content="LinkedIn" />
            </head><body></body></html>
        HTML;

        $result = $this->scrapeWithHtml('https://linkedin.com/jobs/view/999', $html);

        $this->assertSame('UX Designer', $result['jobTitle']);
        $this->assertSame('LinkedIn', $result['companyName']);
        $this->assertSame('linkedin', $result['source']);
    }

    // ─── JSON-LD fallback (Himalayas, Wellfound, Collective.work…) ───────────

    public function testFallbackParsesJsonLdJobPosting(): void
    {
        $html = <<<'HTML'
            <html><head>
                <script type="application/ld+json">
                {
                    "@context": "https://schema.org",
                    "@type": "JobPosting",
                    "title": "Senior Backend Engineer",
                    "hiringOrganization": { "name": "Himalayas Inc" },
                    "jobLocation": [{ "address": { "addressLocality": "Remote" } }],
                    "employmentType": "FULL_TIME",
                    "baseSalary": {
                        "currency": "USD",
                        "value": { "minValue": 120000, "maxValue": 160000 }
                    },
                    "description": "<p>We are looking for a backend engineer.</p>"
                }
                </script>
            </head><body></body></html>
        HTML;

        $result = $this->scrapeWithHtml('https://himalayas.app/jobs/senior-backend', $html);

        $this->assertSame('Senior Backend Engineer', $result['jobTitle']);
        $this->assertSame('Himalayas Inc', $result['companyName']);
        $this->assertSame('Remote', $result['location']);
        $this->assertSame('CDI', $result['contractType']);
        $this->assertSame('120000-160000 USD', $result['salaryRange']);
        $this->assertStringContainsString('backend engineer', $result['jobDescription']);
        $this->assertSame('other', $result['source']);
    }

    // ─── Playwright fallback ──────────────────────────────────────────────────

    public function testPlaywrightFallbackUsedWhenDirectFetchReturnsNoTitle(): void
    {
        $playwrightHtml = <<<'HTML'
            <html><head>
                <script type="application/ld+json">
                {
                    "@type": "JobPosting",
                    "title": "Frontend Engineer",
                    "hiringOrganization": { "name": "Wellfound" }
                }
                </script>
            </head><body></body></html>
        HTML;

        // Direct fetch: returns HTML with no job data
        $directResponse = $this->createStub(ResponseInterface::class);
        $directResponse->method('getStatusCode')->willReturn(200);
        $directResponse->method('getContent')->willReturn('<html><body></body></html>');

        // Playwright /render call: returns full HTML
        $playwrightResponse = $this->createStub(ResponseInterface::class);
        $playwrightResponse->method('getStatusCode')->willReturn(200);
        $playwrightResponse->method('toArray')->willReturn(['html' => $playwrightHtml]);

        $httpClient = $this->createMock(HttpClientInterface::class);
        $httpClient->expects($this->exactly(2))
            ->method('request')
            ->willReturnOnConsecutiveCalls($directResponse, $playwrightResponse);

        $service = new ScraperService($httpClient, 'http://playwright:3001');
        $result = $service->scrape('https://wellfound.com/jobs/frontend-123');

        $this->assertSame('Frontend Engineer', $result['jobTitle']);
        $this->assertSame('Wellfound', $result['companyName']);
    }

    public function testPlaywrightFallbackUsedWhenDirectFetchBlocked(): void
    {
        $playwrightHtml = <<<'HTML'
            <html><head>
                <script type="application/ld+json">
                {"@type":"JobPosting","title":"DevOps Lead","hiringOrganization":{"name":"Acme"}}
                </script>
            </head><body></body></html>
        HTML;

        $blockedResponse = $this->createStub(ResponseInterface::class);
        $blockedResponse->method('getStatusCode')->willReturn(403);
        $blockedResponse->method('getContent')->willReturn('Forbidden');

        $playwrightResponse = $this->createStub(ResponseInterface::class);
        $playwrightResponse->method('getStatusCode')->willReturn(200);
        $playwrightResponse->method('toArray')->willReturn(['html' => $playwrightHtml]);

        $httpClient = $this->createMock(HttpClientInterface::class);
        $httpClient->expects($this->exactly(2))
            ->method('request')
            ->willReturnOnConsecutiveCalls($blockedResponse, $playwrightResponse);

        $service = new ScraperService($httpClient, 'http://playwright:3001');
        $result = $service->scrape('https://example.com/job/1');

        $this->assertSame('DevOps Lead', $result['jobTitle']);
        $this->assertSame('Acme', $result['companyName']);
    }

    public function testThrowsWhenBlockedAndNoPlaywright(): void
    {
        $blockedResponse = $this->createStub(ResponseInterface::class);
        $blockedResponse->method('getStatusCode')->willReturn(403);
        $blockedResponse->method('getContent')->willReturn('Forbidden');

        $httpClient = $this->createStub(HttpClientInterface::class);
        $httpClient->method('request')->willReturn($blockedResponse);

        $service = new ScraperService($httpClient); // no Playwright

        $this->expectException(RuntimeException::class);

        $service->scrape('https://example.com/job/1');
    }

    // ─── Generic fallback ─────────────────────────────────────────────────────

    public function testFallbackExtractsOgMeta(): void
    {
        $html = <<<'HTML'
            <html><head>
                <meta property="og:title" content="Data Scientist" />
                <meta property="og:site_name" content="JobBoard" />
                <meta property="og:description" content="Analyse de données et ML." />
            </head><body><h1>Data Scientist</h1></body></html>
        HTML;

        $result = $this->scrapeWithHtml('https://somejobboard.com/jobs/123', $html);

        $this->assertSame('Data Scientist', $result['jobTitle']);
        $this->assertSame('JobBoard', $result['companyName']);
        $this->assertSame('Analyse de données et ML.', $result['jobDescription']);
        $this->assertSame('other', $result['source']);
    }

    public function testFallbackFallsBackToH1WhenNoOgTitle(): void
    {
        $html = '<html><body><h1>Frontend Developer</h1></body></html>';

        $result = $this->scrapeWithHtml('https://other.com/jobs/1', $html);

        $this->assertSame('Frontend Developer', $result['jobTitle']);
        $this->assertNull($result['companyName']);
    }

    public function testFallbackReturnsNullsForEmptyPage(): void
    {
        $result = $this->scrapeWithHtml('https://other.com/jobs/1', '<html><body></body></html>');

        $this->assertNull($result['jobTitle']);
        $this->assertNull($result['companyName']);
        $this->assertNull($result['location']);
        $this->assertNull($result['contractType']);
        $this->assertNull($result['salaryRange']);
        $this->assertNull($result['jobDescription']);
    }

    // ─── Contract type normalisation ──────────────────────────────────────────

    public function testNormalizesContractTypeFromFrench(): void
    {
        $cases = [
            'CDI – Temps plein' => 'CDI',
            'CDD 6 mois' => 'CDD',
            'Alternance' => 'ALTERNANCE',
            'Stage de 6 mois' => 'INTERNSHIP',
            'Freelance / Mission' => 'FREELANCE',
        ];

        foreach ($cases as $input => $expected) {
            $html = sprintf(
                '<html><body><h1 data-testid="job-header-title">Dev</h1><a data-testid="company-name">Co</a><div data-testid="job-contract-type"><span>%s</span></div></body></html>',
                $input,
            );
            $result = $this->scrapeWithHtml('https://www.welcometothejungle.com/jobs/1', $html);
            $this->assertSame($expected, $result['contractType'], "Failed for input: $input");
        }
    }

    public function testUnrecognizedContractTypeReturnsNull(): void
    {
        $html = '<html><body>
            <h1 data-testid="job-header-title">Dev</h1>
            <a data-testid="company-name">Co</a>
            <div data-testid="job-contract-type"><span>Contrat inconnu</span></div>
        </body></html>';

        $result = $this->scrapeWithHtml('https://www.welcometothejungle.com/jobs/1', $html);

        $this->assertNull($result['contractType']);
    }

    // ─── HTTP error ───────────────────────────────────────────────────────────

    public function testThrowsRuntimeExceptionOnTransportError(): void
    {
        $httpClient = $this->createMock(HttpClientInterface::class);
        $httpClient->method('request')
            ->willThrowException(new \Symfony\Component\HttpClient\Exception\TransportException('Connection refused'));

        $service = new ScraperService($httpClient);

        $this->expectException(RuntimeException::class);
        $this->expectExceptionMessageMatches('/Unable to fetch URL/');

        $service->scrape('https://example.com/job/1');
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private function scrapeWithHtml(string $url, string $html): array
    {
        $response = $this->createStub(ResponseInterface::class);
        $response->method('getStatusCode')->willReturn(200);
        $response->method('getContent')->willReturn($html);

        $httpClient = $this->createStub(HttpClientInterface::class);
        $httpClient->method('request')->willReturn($response);

        $service = new ScraperService($httpClient);

        return $service->scrape($url);
    }
}
