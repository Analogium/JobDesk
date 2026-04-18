<?php

namespace App\Controller;

use App\Service\ScraperService;
use RuntimeException;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

#[IsGranted('ROLE_USER')]
class ScrapeController extends AbstractController
{
    public function __construct(private readonly ScraperService $scraper)
    {
    }

    #[Route('/api/scrape', name: 'api_scrape', methods: ['POST'])]
    public function scrape(Request $request): JsonResponse
    {
        $data = json_decode($request->getContent(), true);
        $url = $data['url'] ?? null;

        if (!\is_string($url) || '' === $url) {
            return $this->json(['error' => 'Missing or invalid "url" field.'], Response::HTTP_BAD_REQUEST);
        }

        // Percent-encode non-ASCII characters before validation (e.g. accented chars in query strings)
        $encodedUrl = preg_replace_callback('/[^\x20-\x7E]/', static fn ($m) => rawurlencode($m[0]), $url) ?? $url;
        if (!filter_var($encodedUrl, FILTER_VALIDATE_URL)) {
            return $this->json(['error' => 'Invalid URL format.'], Response::HTTP_BAD_REQUEST);
        }

        try {
            $result = $this->scraper->scrape($url);
        } catch (RuntimeException $e) {
            return $this->json(['error' => $e->getMessage()], Response::HTTP_BAD_GATEWAY);
        }

        return $this->json($result);
    }
}
