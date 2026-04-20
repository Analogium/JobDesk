<?php

namespace App\Tests\Unit\Service;

use App\Entity\Application;
use App\Entity\User;
use App\Enum\ApplicationStatus;
use App\Repository\ApplicationRepository;
use App\Service\MailScanService;
use Doctrine\ORM\EntityManagerInterface;
use PHPUnit\Framework\MockObject\MockObject;
use PHPUnit\Framework\TestCase;
use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Contracts\HttpClient\ResponseInterface;

class MailScanServiceTest extends TestCase
{
    private MailScanService $service;
    private EntityManagerInterface&MockObject $em;
    private ApplicationRepository&MockObject $applicationRepository;
    private HttpClientInterface&MockObject $httpClient;

    protected function setUp(): void
    {
        $this->em = $this->createMock(EntityManagerInterface::class);
        $this->applicationRepository = $this->createMock(ApplicationRepository::class);
        $this->httpClient = $this->createMock(HttpClientInterface::class);

        $this->service = new MailScanService(
            $this->em,
            $this->applicationRepository,
            $this->httpClient,
            'fake-client-id',
            'fake-client-secret',
        );
    }

    // ─── detectStatus (via scanForUser integration) ───────────────────────────

    public function testDetectsRefusedFromFrenchKeyword(): void
    {
        $result = $this->runScanWithSubjectAndFrom(
            'Nous avons le regret de vous informer que votre candidature',
            'rh@acmecorp.com',
            'Acme Corp',
        );

        $this->assertSame(1, $result['matchesFound']);
    }

    public function testDetectsInterviewFromEnglishKeyword(): void
    {
        $result = $this->runScanWithSubjectAndFrom(
            'We would like to schedule an interview with you',
            'talent@acmecorp.com',
            'Acme Corp',
        );

        $this->assertSame(1, $result['matchesFound']);
    }

    public function testDetectsOfferKeyword(): void
    {
        $result = $this->runScanWithSubjectAndFrom(
            'Pleased to offer you the position of Developer',
            'hr@acmecorp.com',
            'Acme Corp',
        );

        $this->assertSame(1, $result['matchesFound']);
    }

    public function testNoMatchWhenSubjectHasNoKeyword(): void
    {
        $result = $this->runScanWithSubjectAndFrom(
            'Merci de votre candidature chez Acme Corp',
            'noreply@acmecorp.com',
            'Acme Corp',
        );

        $this->assertSame(0, $result['matchesFound']);
    }

    public function testNoMatchWhenCompanyDoesNotMatchSender(): void
    {
        $result = $this->runScanWithSubjectAndFrom(
            'Nous avons le regret de vous informer',
            'rh@other-company.com',
            'Acme Corp',
        );

        $this->assertSame(0, $result['matchesFound']);
    }

    public function testSkipsWhenStatusAlreadyMatches(): void
    {
        $result = $this->runScanWithSubjectAndFrom(
            'Nous avons le regret de vous informer',
            'rh@acmecorp.com',
            'Acme Corp',
            ApplicationStatus::REFUSED,
        );

        $this->assertSame(0, $result['matchesFound']);
    }

    public function testMatchesBySubjectContainingCompanyName(): void
    {
        $result = $this->runScanWithSubjectAndFrom(
            'Acme Corp : entretien prévu demain',
            'noreply@calendly.com',
            'Acme Corp',
        );

        $this->assertSame(1, $result['matchesFound']);
    }

    public function testNoApplicationsReturnsZeroScanned(): void
    {
        $user = $this->makeUserWithGmailToken();
        $this->applicationRepository->method('findBy')->willReturn([]);

        $this->em->expects($this->once())->method('flush');
        $this->httpClient->expects($this->never())->method('request');

        $result = $this->service->scanForUser($user);

        $this->assertSame(0, $result['mailsAnalyzed']);
        $this->assertSame(0, $result['matchesFound']);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /** @return array{mailsAnalyzed: int, matchesFound: int} */
    private function runScanWithSubjectAndFrom(
        string $subject,
        string $from,
        string $companyName,
        ApplicationStatus $initialStatus = ApplicationStatus::APPLIED,
    ): array {
        $user = $this->makeUserWithGmailToken();

        $application = (new Application())
            ->setCompanyName($companyName)
            ->setJobTitle('Developer')
            ->setStatus($initialStatus)
            ->setUser($user);

        $this->applicationRepository->method('findBy')->willReturn([$application]);
        $this->em->method('persist');
        $this->em->method('flush');

        // List messages response
        $listResponse = $this->createMock(ResponseInterface::class);
        $listResponse->method('toArray')->willReturn(['messages' => [['id' => 'msg1']]]);

        // Detail response
        $detailResponse = $this->createMock(ResponseInterface::class);
        $detailResponse->method('toArray')->willReturn([
            'snippet' => '',
            'payload' => [
                'headers' => [
                    ['name' => 'Subject', 'value' => $subject],
                    ['name' => 'From', 'value' => $from],
                ],
            ],
        ]);

        $this->httpClient->method('request')->willReturnOnConsecutiveCalls(
            $listResponse,
            $detailResponse,
        );

        return $this->service->scanForUser($user);
    }

    private function makeUserWithGmailToken(): User
    {
        $user = new User();
        $user->setEmail('user@example.com');
        $user->setName('Test User');
        $user->setGmailToken('fake-access-token');
        $user->setGmailRefreshToken(null); // no refresh needed in unit tests

        return $user;
    }
}
