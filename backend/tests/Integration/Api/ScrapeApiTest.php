<?php

namespace App\Tests\Integration\Api;

use App\Entity\User;
use App\Service\ScraperService;
use Doctrine\ORM\EntityManagerInterface;
use Lexik\Bundle\JWTAuthenticationBundle\Services\JWTTokenManagerInterface;
use RuntimeException;
use Symfony\Bundle\FrameworkBundle\KernelBrowser;
use Symfony\Bundle\FrameworkBundle\Test\WebTestCase;

class ScrapeApiTest extends WebTestCase
{
    private KernelBrowser $client;
    private EntityManagerInterface $em;

    protected function setUp(): void
    {
        $this->client = static::createClient();
        $this->em = static::getContainer()->get('doctrine.orm.entity_manager');
        $this->em->getConnection()->executeStatement('DELETE FROM "user"');
    }

    protected function tearDown(): void
    {
        $this->em->getConnection()->executeStatement('DELETE FROM "user"');
        parent::tearDown();
    }

    // ─── Tests ───────────────────────────────────────────────────────────────

    public function testRequiresAuthentication(): void
    {
        $this->client->request('POST', '/api/scrape', [], [], [
            'CONTENT_TYPE' => 'application/json',
        ], json_encode(['url' => 'https://example.com']));

        $this->assertResponseStatusCodeSame(401);
    }

    public function testReturnsBadRequestWhenUrlMissing(): void
    {
        $user = $this->createUser();

        $this->client->request('POST', '/api/scrape', [], [], [
            'HTTP_AUTHORIZATION' => 'Bearer '.$this->token($user),
            'CONTENT_TYPE' => 'application/json',
        ], json_encode([]));

        $this->assertResponseStatusCodeSame(400);
    }

    public function testReturnsBadRequestWhenUrlInvalid(): void
    {
        $user = $this->createUser();

        $this->client->request('POST', '/api/scrape', [], [], [
            'HTTP_AUTHORIZATION' => 'Bearer '.$this->token($user),
            'CONTENT_TYPE' => 'application/json',
        ], json_encode(['url' => 'not-a-url']));

        $this->assertResponseStatusCodeSame(400);
    }

    public function testReturnsScrapedDataForValidUrl(): void
    {
        $user = $this->createUser();

        $expected = [
            'companyName' => 'Acme Corp',
            'jobTitle' => 'Backend Developer',
            'location' => 'Paris',
            'contractType' => 'CDI',
            'salaryRange' => '50k€',
            'jobDescription' => 'Great opportunity.',
            'source' => 'wttj',
        ];

        $scraperMock = $this->createMock(ScraperService::class);
        $scraperMock->expects($this->once())
            ->method('scrape')
            ->with('https://www.welcometothejungle.com/fr/jobs/abc')
            ->willReturn($expected);

        static::getContainer()->set(ScraperService::class, $scraperMock);

        $this->client->request('POST', '/api/scrape', [], [], [
            'HTTP_AUTHORIZATION' => 'Bearer '.$this->token($user),
            'CONTENT_TYPE' => 'application/json',
        ], json_encode(['url' => 'https://www.welcometothejungle.com/fr/jobs/abc']));

        $this->assertResponseIsSuccessful();

        $data = json_decode($this->client->getResponse()->getContent(), true);

        $this->assertSame('Acme Corp', $data['companyName']);
        $this->assertSame('Backend Developer', $data['jobTitle']);
        $this->assertSame('CDI', $data['contractType']);
        $this->assertSame('wttj', $data['source']);
    }

    public function testReturnsBadGatewayWhenScraperFails(): void
    {
        $user = $this->createUser();

        $scraperMock = $this->createMock(ScraperService::class);
        $scraperMock->method('scrape')
            ->willThrowException(new RuntimeException('Unable to fetch URL: Connection refused'));

        static::getContainer()->set(ScraperService::class, $scraperMock);

        $this->client->request('POST', '/api/scrape', [], [], [
            'HTTP_AUTHORIZATION' => 'Bearer '.$this->token($user),
            'CONTENT_TYPE' => 'application/json',
        ], json_encode(['url' => 'https://example.com/job/1']));

        $this->assertResponseStatusCodeSame(502);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private function createUser(string $email = 'test@example.com', string $name = 'Test'): User
    {
        $user = (new User())->setEmail($email)->setName($name);
        $this->em->persist($user);
        $this->em->flush();

        return $user;
    }

    private function token(User $user): string
    {
        /** @var JWTTokenManagerInterface $mgr */
        $mgr = static::getContainer()->get(JWTTokenManagerInterface::class);

        return $mgr->create($user);
    }
}
