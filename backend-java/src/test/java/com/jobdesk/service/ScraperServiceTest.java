package com.jobdesk.service;

import com.jobdesk.web.dto.ScrapeResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScraperServiceTest {

    private final ScraperService scraper = new ScraperService("");

    @Test
    void normalizeContractTypeMapsSchemaAndKeywords() {
        assertThat(scraper.normalizeContractType("FULL_TIME")).isEqualTo("CDI");
        assertThat(scraper.normalizeContractType("CONTRACTOR")).isEqualTo("FREELANCE");
        assertThat(scraper.normalizeContractType("INTERN")).isEqualTo("INTERNSHIP");
        assertThat(scraper.normalizeContractType("CDD - 6 mois")).isEqualTo("CDD");
        assertThat(scraper.normalizeContractType("Alternance")).isEqualTo("ALTERNANCE");
        assertThat(scraper.normalizeContractType("Stage de fin d'études")).isEqualTo("INTERNSHIP");
        assertThat(scraper.normalizeContractType("Blabla")).isNull();
        assertThat(scraper.normalizeContractType(null)).isNull();
    }

    @Test
    void parsesJobPostingJsonLd() {
        String html = """
                <html><head>
                <script type="application/ld+json">
                {
                  "@context": "https://schema.org",
                  "@type": "JobPosting",
                  "title": "Senior Java Engineer (CDI)",
                  "hiringOrganization": {"name": "ACME Corp"},
                  "jobLocation": {"address": {"addressLocality": "Paris"}},
                  "employmentType": "FULL_TIME",
                  "baseSalary": {"currency": "EUR", "value": {"minValue": 45000, "maxValue": 60000}},
                  "description": "<p>Great job</p><p>Join us</p>"
                }
                </script></head><body></body></html>
                """;
        ScrapeResult r = scraper.parse(html, "https://boards.example.com/jobs/42");

        assertThat(r.jobTitle()).isEqualTo("Senior Java Engineer"); // suffixe (CDI) retiré
        assertThat(r.companyName()).isEqualTo("ACME Corp");
        assertThat(r.location()).isEqualTo("Paris");
        assertThat(r.contractType()).isEqualTo("CDI");
        assertThat(r.salaryRange()).isEqualTo("45000-60000 EUR");
        assertThat(r.jobDescription()).contains("Great job").contains("Join us");
        assertThat(r.source()).isEqualTo("other");
    }

    @Test
    void fallsBackToOpenGraph() {
        String html = """
                <html><head>
                <meta property="og:title" content="Product Manager">
                <meta property="og:site_name" content="CoolStartup">
                <meta property="og:description" content="We are hiring a PM">
                </head><body></body></html>
                """;
        ScrapeResult r = scraper.parse(html, "https://unknown.example.com/p/1");

        assertThat(r.jobTitle()).isEqualTo("Product Manager");
        assertThat(r.companyName()).isEqualTo("CoolStartup");
        assertThat(r.jobDescription()).isEqualTo("We are hiring a PM");
    }

    @Test
    void jsonLdInsideGraphArray() {
        String html = """
                <script type="application/ld+json">
                {"@context":"https://schema.org","@graph":[
                  {"@type":"WebSite","name":"x"},
                  {"@type":"JobPosting","title":"Data Analyst","hiringOrganization":{"name":"DataCo"}}
                ]}
                </script>
                """;
        ScrapeResult r = scraper.parse(html, "https://x.example.com/j");
        assertThat(r.jobTitle()).isEqualTo("Data Analyst");
        assertThat(r.companyName()).isEqualTo("DataCo");
    }
}
