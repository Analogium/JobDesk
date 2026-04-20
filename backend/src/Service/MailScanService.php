<?php

namespace App\Service;

use App\Entity\Application;
use App\Entity\MailScan;
use App\Entity\StatusHistory;
use App\Entity\User;
use App\Enum\ApplicationStatus;
use App\Repository\ApplicationRepository;
use DateTimeImmutable;
use Doctrine\ORM\EntityManagerInterface;
use League\OAuth2\Client\Provider\Google;
use Symfony\Contracts\HttpClient\HttpClientInterface;
use Throwable;

class MailScanService
{
    private const GMAIL_API = 'https://gmail.googleapis.com/gmail/v1';
    private const MAX_RESULTS = 100;

    private const KEYWORDS_REFUSED = [
        // FR
        'nous avons le regret', 'avons le regret', 'regret de vous informer',
        'n\'avons pas retenu', 'n\'a pas été retenu', 'pas été retenue',
        'ne pas retenir', 'ne retenons pas', 'ne donnons pas suite',
        'ne donner pas suite', 'sans suite', 'avons décidé de ne pas poursuivre',
        'ne correspond pas à', 'votre profil ne correspond', 'candidature n\'a pas',
        'après étude de votre candidature', 'poursuivre avec d\'autres candidats',
        'nous ne sommes pas en mesure', 'n\'avons pas pu retenir', 'refus',
        // EN
        'unfortunately', 'regret to inform', 'not moving forward',
        'not selected', 'we will not be', 'decided not to proceed',
        'not a match', 'we have decided to', 'we won\'t be moving',
        'other candidates', 'position has been filled', 'no longer considering',
        'not the right fit', 'decided to move forward with other',
        'we regret', 'not successful',
    ];

    private const KEYWORDS_INTERVIEW = [
        // FR
        'entretien', 'rendez-vous', 'visioconférence', 'visio',
        'nous aimerions vous rencontrer', 'souhaitons échanger avec vous',
        'disponibilités', 'créneaux', 'prise de contact', 'appel téléphonique',
        'call téléphonique', 'vous rencontrer', 'échanger avec vous',
        'convocation', 'invitation à un entretien',
        // EN
        'interview', 'call with', 'meeting with', 'schedule a call',
        'schedule an interview', 'phone screen', 'phone call', 'video call',
        'would like to meet', 'set up a time', 'find a time', 'book a slot',
        'invite you to', 'next steps',
        // Tools
        'teams meeting', 'google meet', 'zoom', 'calendly',
    ];

    private const KEYWORDS_OFFER = [
        // FR
        'offre d\'emploi', 'proposition d\'embauche', 'nous vous proposons',
        'bienvenue dans', 'nous souhaitons vous accueillir',
        'lettre d\'engagement', 'heureux de vous proposer',
        'ravis de vous accueillir', 'rejoindre notre équipe',
        'offre de poste', 'vous rejoindre',
        // EN
        'pleased to offer', 'offer of employment', 'welcome to the team',
        'job offer', 'formal offer', 'offer letter', 'we would like to offer',
        'extend an offer', 'happy to offer', 'excited to offer',
        'we are delighted to offer',
    ];

    public function __construct(
        private readonly EntityManagerInterface $em,
        private readonly ApplicationRepository $applicationRepository,
        private readonly HttpClientInterface $httpClient,
        private readonly string $googleClientId,
        private readonly string $googleClientSecret,
    ) {
    }

    /** @return array{mailsAnalyzed: int, matchesFound: int, error?: string} */
    public function scanForUser(User $user): array
    {
        $accessToken = $this->refreshTokenIfNeeded($user);
        if (null === $accessToken) {
            $this->logAndFlush($user, 0, 0, 'error', 'Impossible de rafraîchir le token Gmail');

            return ['mailsAnalyzed' => 0, 'matchesFound' => 0, 'error' => 'Impossible de rafraîchir le token Gmail'];
        }

        $since = $user->getLastMailScanAt() ?? new DateTimeImmutable('-7 days');
        $applications = $this->applicationRepository->findBy(['user' => $user]);

        if (empty($applications)) {
            $user->setLastMailScanAt(new DateTimeImmutable());
            $this->em->flush();

            return ['mailsAnalyzed' => 0, 'matchesFound' => 0];
        }

        try {
            $messages = $this->fetchMessages($accessToken, $since);
        } catch (Throwable $e) {
            $this->logAndFlush($user, 0, 0, 'error', $e->getMessage());

            return ['mailsAnalyzed' => 0, 'matchesFound' => 0, 'error' => $e->getMessage()];
        }

        $matchesFound = 0;
        foreach ($messages as $message) {
            try {
                $detail = $this->fetchMessageDetail($accessToken, $message['id']);
                $matched = $this->processMessage($detail, $applications, $user);
                if ($matched) {
                    ++$matchesFound;
                }
            } catch (Throwable) {
                // skip malformed messages
            }
        }

        $user->setLastMailScanAt(new DateTimeImmutable());
        $this->logScan($user, count($messages), $matchesFound, 'success');
        $this->em->flush();

        return ['mailsAnalyzed' => count($messages), 'matchesFound' => $matchesFound];
    }

    // ─── Gmail API ────────────────────────────────────────────────────────────

    private function refreshTokenIfNeeded(User $user): ?string
    {
        $refreshToken = $user->getGmailRefreshToken();
        if (null === $refreshToken) {
            return $user->getGmailToken();
        }

        try {
            $provider = new Google([
                'clientId' => $this->googleClientId,
                'clientSecret' => $this->googleClientSecret,
                'redirectUri' => '',
            ]);

            $newToken = $provider->getAccessToken('refresh_token', [
                'refresh_token' => $refreshToken,
            ]);

            $user->setGmailToken($newToken->getToken());
            $this->em->flush();

            return $newToken->getToken();
        } catch (Throwable) {
            return $user->getGmailToken();
        }
    }

    /** @return array<array{id: string}> */
    private function fetchMessages(string $accessToken, DateTimeImmutable $since): array
    {
        $query = 'after:'.$since->format('Y/m/d');

        $response = $this->httpClient->request('GET', self::GMAIL_API.'/users/me/messages', [
            'headers' => ['Authorization' => 'Bearer '.$accessToken],
            'query' => ['q' => $query, 'maxResults' => self::MAX_RESULTS],
        ]);

        $data = $response->toArray(false);

        if (isset($data['error'])) {
            $msg = $data['error']['message'] ?? $data['error']['status'] ?? json_encode($data['error']);
            throw new \RuntimeException('Gmail API: '.$msg);
        }

        return $data['messages'] ?? [];
    }

    /** @return array{subject: string, from: string} */
    private function fetchMessageDetail(string $accessToken, string $messageId): array
    {
        $response = $this->httpClient->request('GET', self::GMAIL_API.'/users/me/messages/'.$messageId, [
            'headers' => ['Authorization' => 'Bearer '.$accessToken],
            'query' => [
                'format' => 'metadata',
                'metadataHeaders' => ['Subject', 'From'],
            ],
        ]);

        $data = $response->toArray();
        $headers = $data['payload']['headers'] ?? [];

        $subject = '';
        $from = '';
        foreach ($headers as $header) {
            if ('Subject' === $header['name']) {
                $subject = $header['value'];
            } elseif ('From' === $header['name']) {
                $from = $header['value'];
            }
        }

        return ['subject' => $subject, 'from' => $from, 'snippet' => $data['snippet'] ?? ''];
    }

    // ─── Matching ─────────────────────────────────────────────────────────────

    /** @param Application[] $applications */
    private function processMessage(array $message, array $applications, User $user): bool
    {
        $subject = mb_strtolower($message['subject']);
        $from = mb_strtolower($message['from']);
        $snippet = mb_strtolower($message['snippet']);
        $fromDomain = $this->extractDomain($from);

        foreach ($applications as $application) {
            if (!$this->matchesApplication($application, $subject, $from, $snippet, $fromDomain)) {
                continue;
            }

            $detectedStatus = $this->detectStatus($subject.' '.$snippet);
            if (null === $detectedStatus) {
                continue;
            }

            if ($detectedStatus === $application->getStatus()) {
                continue;
            }

            $this->updateApplicationStatus($application, $detectedStatus, $message['subject']);

            return true;
        }

        return false;
    }

    private function matchesApplication(
        Application $application,
        string $subject,
        string $from,
        string $snippet,
        string $fromDomain,
    ): bool {
        $normalizedCompany = $this->normalizeCompanyName($application->getCompanyName() ?? '');

        if ('' === $normalizedCompany) {
            return false;
        }

        // Check domain match
        if ('' !== $fromDomain && str_contains($fromDomain, $normalizedCompany)) {
            return true;
        }

        // Check company name in subject, from, or body snippet
        if (str_contains($subject, $normalizedCompany)
            || str_contains($from, $normalizedCompany)
            || str_contains($snippet, $normalizedCompany)) {
            return true;
        }

        // Check each word of the company name (for multi-word names)
        $words = array_filter(explode(' ', $normalizedCompany), fn (string $w) => mb_strlen($w) > 3);
        foreach ($words as $word) {
            if (str_contains($fromDomain, $word) || str_contains($subject, $word) || str_contains($snippet, $word)) {
                return true;
            }
        }

        return false;
    }

    private function detectStatus(string $subject): ?ApplicationStatus
    {
        $lower = mb_strtolower($subject);

        foreach (self::KEYWORDS_REFUSED as $kw) {
            if (str_contains($lower, $kw)) {
                return ApplicationStatus::REFUSED;
            }
        }

        foreach (self::KEYWORDS_OFFER as $kw) {
            if (str_contains($lower, $kw)) {
                return ApplicationStatus::OFFER;
            }
        }

        foreach (self::KEYWORDS_INTERVIEW as $kw) {
            if (str_contains($lower, $kw)) {
                return ApplicationStatus::INTERVIEW;
            }
        }

        return null;
    }

    private function updateApplicationStatus(
        Application $application,
        ApplicationStatus $newStatus,
        string $subjectNote,
    ): void {
        $previousStatus = $application->getStatus();
        $application->setStatus($newStatus);

        $history = new StatusHistory();
        $history->setApplication($application);
        $history->setPreviousStatus($previousStatus);
        $history->setNewStatus($newStatus);
        $history->setTrigger('auto_mail');
        $history->setNotes('Détecté depuis : '.$subjectNote);

        $this->em->persist($history);
    }

    private function logScan(
        User $user,
        int $mailsAnalyzed,
        int $matchesFound,
        string $status,
        ?string $errorMessage = null,
    ): void {
        $scan = new MailScan();
        $scan->setUser($user);
        $scan->setMailsAnalyzed($mailsAnalyzed);
        $scan->setMatchesFound($matchesFound);
        $scan->setStatus($status);
        $scan->setErrorMessage($errorMessage);

        $this->em->persist($scan);
    }

    private function logAndFlush(
        User $user,
        int $mailsAnalyzed,
        int $matchesFound,
        string $status,
        ?string $errorMessage = null,
    ): void {
        $this->logScan($user, $mailsAnalyzed, $matchesFound, $status, $errorMessage);
        $this->em->flush();
    }

    // ─── Utils ────────────────────────────────────────────────────────────────

    private function extractDomain(string $from): string
    {
        if (preg_match('/<([^>]+)>/', $from, $m)) {
            $email = $m[1];
        } elseif (str_contains($from, '@')) {
            $email = trim($from);
        } else {
            return '';
        }

        $parts = explode('@', $email);

        return mb_strtolower(trim(end($parts)));
    }

    private function normalizeCompanyName(string $name): string
    {
        $name = mb_strtolower($name);
        $name = (string) transliterator_transliterate('Any-Latin; Latin-ASCII', $name);
        // Remove legal suffixes
        $name = preg_replace('/\b(sas|sarl|sa|sasu|ltd|inc|gmbh|bv|nv|llc|corp|group|groupe)\b/', '', $name) ?? $name;
        // Keep only alphanumeric and spaces
        $name = preg_replace('/[^a-z0-9 ]/', '', $name) ?? $name;

        return trim((string) preg_replace('/\s+/', ' ', $name));
    }
}
