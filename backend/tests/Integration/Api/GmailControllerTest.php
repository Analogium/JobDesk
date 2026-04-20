<?php

namespace App\Tests\Integration\Api;

use App\Entity\User;
use Doctrine\ORM\EntityManagerInterface;
use Lexik\Bundle\JWTAuthenticationBundle\Services\JWTTokenManagerInterface;
use Symfony\Bundle\FrameworkBundle\KernelBrowser;
use Symfony\Bundle\FrameworkBundle\Test\WebTestCase;

class GmailControllerTest extends WebTestCase
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

    // ─── /api/gmail/status ────────────────────────────────────────────────────

    public function testStatusRequiresAuth(): void
    {
        $this->client->request('GET', '/api/gmail/status');
        $this->assertResponseStatusCodeSame(401);
    }

    public function testStatusReturnsFalseWhenNotConnected(): void
    {
        $user = $this->createUser('test@example.com');
        $this->request('GET', '/api/gmail/status', $user);

        $this->assertResponseIsSuccessful();
        $data = $this->json();
        $this->assertFalse($data['connected']);
        $this->assertNull($data['lastMailScanAt']);
    }

    public function testStatusReturnsTrueWhenConnected(): void
    {
        $user = $this->createUser('test@example.com');
        $user->setGmailToken('some-token');
        $this->em->flush();

        $this->request('GET', '/api/gmail/status', $user);

        $this->assertResponseIsSuccessful();
        $data = $this->json();
        $this->assertTrue($data['connected']);
    }

    // ─── /api/gmail/disconnect ────────────────────────────────────────────────

    public function testDisconnectRequiresAuth(): void
    {
        $this->client->request('DELETE', '/api/gmail/disconnect');
        $this->assertResponseStatusCodeSame(401);
    }

    public function testDisconnectClearsTokens(): void
    {
        $user = $this->createUser('test@example.com');
        $user->setGmailToken('some-token');
        $user->setGmailRefreshToken('some-refresh');
        $this->em->flush();

        $this->request('DELETE', '/api/gmail/disconnect', $user);

        $this->assertResponseStatusCodeSame(204);

        $this->em->refresh($user);
        $this->assertNull($user->getGmailToken());
        $this->assertNull($user->getGmailRefreshToken());
    }

    // ─── /api/gmail/scan ─────────────────────────────────────────────────────

    public function testScanRequiresAuth(): void
    {
        $this->client->request('POST', '/api/gmail/scan');
        $this->assertResponseStatusCodeSame(401);
    }

    public function testScanReturnsBadRequestWhenNotConnected(): void
    {
        $user = $this->createUser('test@example.com');
        $this->request('POST', '/api/gmail/scan', $user);

        $this->assertResponseStatusCodeSame(400);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private function clearDatabase(): void
    {
        $this->em->createQuery('DELETE FROM App\Entity\MailScan')->execute();
        $this->em->createQuery('DELETE FROM App\Entity\StatusHistory')->execute();
        $this->em->createQuery('DELETE FROM App\Entity\Application')->execute();
        $this->em->createQuery('DELETE FROM App\Entity\User')->execute();
    }

    private function createUser(string $email): User
    {
        $user = new User();
        $user->setEmail($email);
        $user->setName('Test User');
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

    private function request(string $method, string $uri, ?User $user = null): void
    {
        $headers = ['HTTP_ACCEPT' => 'application/json'];
        if (null !== $user) {
            $headers['HTTP_AUTHORIZATION'] = 'Bearer '.$this->token($user);
        }
        $this->client->request($method, $uri, [], [], $headers);
    }

    private function json(): array
    {
        return json_decode($this->client->getResponse()->getContent(), true);
    }
}
