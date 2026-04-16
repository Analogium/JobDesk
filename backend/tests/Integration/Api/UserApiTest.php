<?php

namespace App\Tests\Integration\Api;

use App\Entity\User;
use Doctrine\ORM\EntityManagerInterface;
use Lexik\Bundle\JWTAuthenticationBundle\Services\JWTTokenManagerInterface;
use Symfony\Bundle\FrameworkBundle\KernelBrowser;
use Symfony\Bundle\FrameworkBundle\Test\WebTestCase;

class UserApiTest extends WebTestCase
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

    public function testGetMeRequiresAuth(): void
    {
        $this->client->request('GET', '/api/me');
        $this->assertResponseStatusCodeSame(401);
    }

    public function testGetMeReturnsAuthenticatedUser(): void
    {
        $user = $this->createUser('me@example.com', 'Jean Dupont');
        $token = $this->token($user);

        $this->client->request('GET', '/api/me', [], [], [
            'HTTP_AUTHORIZATION' => 'Bearer '.$token,
            'HTTP_ACCEPT' => 'application/ld+json',
        ]);

        $this->assertResponseIsSuccessful();
        $data = json_decode($this->client->getResponse()->getContent(), true);

        $this->assertSame('me@example.com', $data['email']);
        $this->assertSame('Jean Dupont', $data['name']);
        $this->assertArrayHasKey('id', $data);
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
