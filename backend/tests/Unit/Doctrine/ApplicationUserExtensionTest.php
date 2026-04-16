<?php

namespace App\Tests\Unit\Doctrine;

use ApiPlatform\Doctrine\Orm\Util\QueryNameGeneratorInterface;
use App\Doctrine\ApplicationUserExtension;
use App\Entity\Application;
use App\Entity\User;
use Doctrine\ORM\QueryBuilder;
use PHPUnit\Framework\TestCase;
use stdClass;
use Symfony\Bundle\SecurityBundle\Security;

class ApplicationUserExtensionTest extends TestCase
{
    // ─── applyToCollection ───────────────────────────────────────────────────

    public function testAddsWhereClauseForApplicationCollection(): void
    {
        $user = $this->makeUser();
        $extension = $this->makeExtension($user);

        $qb = $this->mockQb('o');
        $qb->expects($this->once())
            ->method('andWhere')
            ->with('o.user = :current_user')
            ->willReturnSelf();
        $qb->expects($this->once())
            ->method('setParameter')
            ->with('current_user', $user)
            ->willReturnSelf();

        $extension->applyToCollection($qb, $this->nameGen(), Application::class);
    }

    public function testSkipsNonApplicationResourceForCollection(): void
    {
        $extension = $this->makeExtension($this->makeUser());

        $qb = $this->mockQb('o');
        $qb->expects($this->never())->method('andWhere');

        $extension->applyToCollection($qb, $this->nameGen(), stdClass::class);
    }

    public function testSkipsWhenUserIsNullForCollection(): void
    {
        $extension = $this->makeExtension(null);

        $qb = $this->mockQb('o');
        $qb->expects($this->never())->method('andWhere');

        $extension->applyToCollection($qb, $this->nameGen(), Application::class);
    }

    // ─── applyToItem ─────────────────────────────────────────────────────────

    public function testAddsWhereClauseForApplicationItem(): void
    {
        $user = $this->makeUser();
        $extension = $this->makeExtension($user);

        $qb = $this->mockQb('o');
        $qb->expects($this->once())->method('andWhere')->willReturnSelf();
        $qb->expects($this->once())->method('setParameter')->willReturnSelf();

        $extension->applyToItem($qb, $this->nameGen(), Application::class, ['id' => 'uuid']);
    }

    public function testSkipsNonApplicationResourceForItem(): void
    {
        $extension = $this->makeExtension($this->makeUser());

        $qb = $this->mockQb('o');
        $qb->expects($this->never())->method('andWhere');

        $extension->applyToItem($qb, $this->nameGen(), stdClass::class, []);
    }

    public function testSkipsWhenUserIsNullForItem(): void
    {
        $extension = $this->makeExtension(null);

        $qb = $this->mockQb('o');
        $qb->expects($this->never())->method('andWhere');

        $extension->applyToItem($qb, $this->nameGen(), Application::class, []);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private function makeUser(string $email = 'test@example.com'): User
    {
        return (new User())->setEmail($email)->setName('Test');
    }

    private function makeExtension(?User $user): ApplicationUserExtension
    {
        $security = $this->createStub(Security::class);
        $security->method('getUser')->willReturn($user);

        return new ApplicationUserExtension($security);
    }

    private function mockQb(string $alias): QueryBuilder
    {
        $qb = $this->createMock(QueryBuilder::class);
        $qb->method('getRootAliases')->willReturn([$alias]);

        return $qb;
    }

    private function nameGen(): QueryNameGeneratorInterface
    {
        return $this->createStub(QueryNameGeneratorInterface::class);
    }
}
