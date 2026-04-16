<?php

namespace App\Tests\Integration\Api;

use App\Entity\Application;
use App\Entity\User;
use App\Enum\ApplicationStatus;
use Doctrine\ORM\EntityManagerInterface;
use Lexik\Bundle\JWTAuthenticationBundle\Services\JWTTokenManagerInterface;
use Symfony\Bundle\FrameworkBundle\KernelBrowser;
use Symfony\Bundle\FrameworkBundle\Test\WebTestCase;

class ApplicationApiTest extends WebTestCase
{
    private KernelBrowser $client;
    private EntityManagerInterface $em;

    protected function setUp(): void
    {
        $this->client = static::createClient();
        $this->em = static::getContainer()->get('doctrine.orm.entity_manager');
        $this->clearDatabase();
    }

    protected function tearDown(): void
    {
        $this->clearDatabase();
        parent::tearDown();
    }

    // ─── Tests ───────────────────────────────────────────────────────────────

    public function testGetCollectionRequiresAuth(): void
    {
        $this->client->request('GET', '/api/applications');
        $this->assertResponseStatusCodeSame(401);
    }

    public function testGetCollectionReturnsOnlyCurrentUserApplications(): void
    {
        $user1 = $this->createUser('user1@example.com');
        $user2 = $this->createUser('user2@example.com');

        $this->createApplication($user1, 'Company A');
        $this->createApplication($user1, 'Company B');
        $this->createApplication($user2, 'Company C'); // must NOT appear

        $this->request('GET', '/api/applications', user: $user1);

        $this->assertResponseIsSuccessful();
        $data = $this->json();

        $this->assertCount(2, $data['member']);
        $companies = array_column($data['member'], 'companyName');
        $this->assertContains('Company A', $companies);
        $this->assertContains('Company B', $companies);
        $this->assertNotContains('Company C', $companies);
    }

    public function testPostCreatesApplicationAssignedToCurrentUser(): void
    {
        $user = $this->createUser();

        $this->request('POST', '/api/applications', user: $user, body: [
            'companyName' => 'Google',
            'jobTitle' => 'Software Engineer',
            'status' => 'APPLIED',
        ]);

        $this->assertResponseStatusCodeSame(201);
        $data = $this->json();
        $this->assertSame('Google', $data['companyName']);
        $this->assertSame('APPLIED', $data['status']);
        $this->assertArrayHasKey('id', $data);

        // Verify in DB that user is correctly assigned
        $this->em->clear();
        $app = $this->em->getRepository(Application::class)->findOneBy(['companyName' => 'Google']);
        $this->assertNotNull($app);
        $this->assertSame($user->getUserIdentifier(), $app->getUser()->getUserIdentifier());
    }

    public function testPostRequiresAuth(): void
    {
        $this->client->request('POST', '/api/applications', [], [], [
            'CONTENT_TYPE' => 'application/json',
        ], json_encode(['companyName' => 'X', 'jobTitle' => 'Y']));

        $this->assertResponseStatusCodeSame(401);
    }

    public function testPatchUpdatesStatusAndCreatesHistory(): void
    {
        $user = $this->createUser();
        $app = $this->createApplication($user, 'Google', ApplicationStatus::APPLIED);

        $this->request(
            'PATCH',
            '/api/applications/'.$app->getId(),
            user: $user,
            body: ['status' => 'INTERVIEW'],
            contentType: 'application/merge-patch+json',
        );

        $this->assertResponseIsSuccessful();
        $data = $this->json();

        $this->assertSame('INTERVIEW', $data['status']);
        $this->assertCount(1, $data['statusHistories']);
        $this->assertSame('APPLIED', $data['statusHistories'][0]['previousStatus']);
        $this->assertSame('INTERVIEW', $data['statusHistories'][0]['newStatus']);
        $this->assertSame('manual', $data['statusHistories'][0]['trigger']);
    }

    public function testPatchByDifferentUserReturns404(): void
    {
        $owner = $this->createUser('owner@example.com');
        $other = $this->createUser('other@example.com');
        $app = $this->createApplication($owner, 'Google');

        $this->request(
            'PATCH',
            '/api/applications/'.$app->getId(),
            user: $other,
            body: ['status' => 'INTERVIEW'],
            contentType: 'application/merge-patch+json',
        );

        $this->assertResponseStatusCodeSame(404);
    }

    public function testDeleteReturns204(): void
    {
        $user = $this->createUser();
        $app = $this->createApplication($user);
        $appId = (string) $app->getId();

        $this->request('DELETE', '/api/applications/'.$appId, user: $user);
        $this->assertResponseStatusCodeSame(204);

        // Should be gone now
        $this->request('GET', '/api/applications/'.$appId, user: $user);
        $this->assertResponseStatusCodeSame(404);
    }

    public function testDeleteByDifferentUserReturns404(): void
    {
        $owner = $this->createUser('owner@example.com');
        $other = $this->createUser('other@example.com');
        $app = $this->createApplication($owner);

        $this->request('DELETE', '/api/applications/'.$app->getId(), user: $other);
        $this->assertResponseStatusCodeSame(404);
    }

    public function testGetItemRequiresAuth(): void
    {
        $user = $this->createUser();
        $app = $this->createApplication($user);

        $this->client->request('GET', '/api/applications/'.$app->getId());
        $this->assertResponseStatusCodeSame(401);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private function clearDatabase(): void
    {
        $conn = $this->em->getConnection();
        $conn->executeStatement('DELETE FROM status_history');
        $conn->executeStatement('DELETE FROM application');
        $conn->executeStatement('DELETE FROM "user"');
    }

    private function createUser(string $email = 'test@example.com'): User
    {
        $user = (new User())->setEmail($email)->setName('Test User');
        $this->em->persist($user);
        $this->em->flush();

        return $user;
    }

    private function createApplication(
        User $user,
        string $company = 'ACME',
        ApplicationStatus $status = ApplicationStatus::APPLIED,
    ): Application {
        $app = (new Application())
            ->setCompanyName($company)
            ->setJobTitle('Developer')
            ->setUser($user)
            ->setStatus($status);
        $this->em->persist($app);
        $this->em->flush();

        return $app;
    }

    private function token(User $user): string
    {
        /** @var JWTTokenManagerInterface $mgr */
        $mgr = static::getContainer()->get(JWTTokenManagerInterface::class);

        return $mgr->create($user);
    }

    /** Sends a JSON request with optional JWT auth. */
    private function request(
        string $method,
        string $uri,
        ?User $user = null,
        array $body = [],
        string $contentType = 'application/json',
    ): void {
        $headers = [
            'CONTENT_TYPE' => $contentType,
            'HTTP_ACCEPT' => 'application/ld+json',
        ];
        if (null !== $user) {
            $headers['HTTP_AUTHORIZATION'] = 'Bearer '.$this->token($user);
        }

        $this->client->request(
            $method,
            $uri,
            [],
            [],
            $headers,
            $body ? json_encode($body) : null,
        );
    }

    private function json(): array
    {
        return json_decode($this->client->getResponse()->getContent(), true);
    }
}
