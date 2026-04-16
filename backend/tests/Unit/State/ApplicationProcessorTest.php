<?php

namespace App\Tests\Unit\State;

use ApiPlatform\Metadata\Patch;
use ApiPlatform\Metadata\Post;
use ApiPlatform\State\ProcessorInterface;
use App\Entity\Application;
use App\Entity\StatusHistory;
use App\Entity\User;
use App\Enum\ApplicationStatus;
use App\State\ApplicationProcessor;
use Doctrine\ORM\EntityManagerInterface;
use Doctrine\ORM\UnitOfWork;
use PHPUnit\Framework\TestCase;
use ReflectionProperty;
use stdClass;
use Symfony\Bundle\SecurityBundle\Security;
use Symfony\Component\Uid\Uuid;

class ApplicationProcessorTest extends TestCase
{
    // ─── Tests ───────────────────────────────────────────────────────────────

    public function testAssignsCurrentUserOnNewApplication(): void
    {
        $user = $this->makeUser('test@example.com');
        $app = $this->makeApp();

        $processor = $this->buildProcessor($user);
        $processor->process($app, new Post());

        $this->assertSame($user, $app->getUser());
    }

    public function testDoesNotOverrideAlreadyAssignedUser(): void
    {
        $existing = $this->makeUser('owner@example.com');
        $app = $this->makeApp()->setUser($existing);

        $processor = $this->buildProcessor($this->makeUser('other@example.com'));
        $processor->process($app, new Post());

        $this->assertSame($existing, $app->getUser());
    }

    public function testCreatesStatusHistoryWhenStatusChanges(): void
    {
        $app = $this->makeAppWithId();
        $app->setStatus(ApplicationStatus::INTERVIEW);

        $uow = $this->createStub(UnitOfWork::class);
        $uow->method('getOriginalEntityData')
            ->willReturn(['status' => ApplicationStatus::APPLIED]);

        $em = $this->createMock(EntityManagerInterface::class);
        $em->method('getUnitOfWork')->willReturn($uow);

        $persisted = null;
        $em->expects($this->once())
            ->method('persist')
            ->with($this->callback(function ($entity) use (&$persisted) {
                $persisted = $entity;

                return $entity instanceof StatusHistory;
            }));
        $em->expects($this->once())->method('flush');

        $inner = $this->createStub(ProcessorInterface::class);
        $inner->method('process')->willReturn($app);

        $processor = new ApplicationProcessor($inner, $this->createStub(Security::class), $em);
        $processor->process($app, new Patch());

        $this->assertInstanceOf(StatusHistory::class, $persisted);
        $this->assertSame(ApplicationStatus::APPLIED, $persisted->getPreviousStatus());
        $this->assertSame(ApplicationStatus::INTERVIEW, $persisted->getNewStatus());
        $this->assertSame('manual', $persisted->getTrigger());
    }

    public function testDoesNotCreateHistoryWhenStatusUnchanged(): void
    {
        $app = $this->makeAppWithId();
        $app->setStatus(ApplicationStatus::APPLIED);

        $uow = $this->createStub(UnitOfWork::class);
        $uow->method('getOriginalEntityData')
            ->willReturn(['status' => ApplicationStatus::APPLIED]);

        $em = $this->createMock(EntityManagerInterface::class);
        $em->method('getUnitOfWork')->willReturn($uow);
        $em->expects($this->never())->method('persist');
        $em->expects($this->never())->method('flush');

        $inner = $this->createStub(ProcessorInterface::class);
        $inner->method('process')->willReturn($app);

        $processor = new ApplicationProcessor($inner, $this->createStub(Security::class), $em);
        $processor->process($app, new Patch());
    }

    public function testDoesNotCreateHistoryForNewApplication(): void
    {
        $app = $this->makeApp();

        $em = $this->createMock(EntityManagerInterface::class);
        $em->expects($this->never())->method('persist');
        $em->expects($this->never())->method('flush');

        $inner = $this->createStub(ProcessorInterface::class);
        $inner->method('process')->willReturn($app);

        $security = $this->createStub(Security::class);
        $security->method('getUser')->willReturn($this->makeUser());

        $processor = new ApplicationProcessor($inner, $security, $em);
        $processor->process($app, new Post());
    }

    public function testPassesThroughNonApplicationData(): void
    {
        $other = new stdClass();

        $inner = $this->createMock(ProcessorInterface::class);
        $inner->expects($this->once())
            ->method('process')
            ->with($other)
            ->willReturn($other);

        $em = $this->createStub(EntityManagerInterface::class);
        $em->method('getUnitOfWork')->willReturn($this->createStub(UnitOfWork::class));

        $processor = new ApplicationProcessor($inner, $this->createStub(Security::class), $em);
        $result = $processor->process($other, new Post());

        $this->assertSame($other, $result);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private function makeUser(string $email = 'test@example.com'): User
    {
        return (new User())->setEmail($email)->setName('Test');
    }

    private function makeApp(): Application
    {
        return (new Application())->setCompanyName('ACME')->setJobTitle('Dev');
    }

    private function makeAppWithId(): Application
    {
        $app = $this->makeApp();
        $ref = new ReflectionProperty(Application::class, 'id');
        $ref->setAccessible(true);
        $ref->setValue($app, Uuid::v4());

        return $app;
    }

    private function buildProcessor(?User $user = null): ApplicationProcessor
    {
        $uow = $this->createStub(UnitOfWork::class);

        $em = $this->createStub(EntityManagerInterface::class);
        $em->method('getUnitOfWork')->willReturn($uow);

        $security = $this->createStub(Security::class);
        if (null !== $user) {
            $security->method('getUser')->willReturn($user);
        }

        $inner = $this->createStub(ProcessorInterface::class);
        $inner->method('process')->willReturnArgument(0);

        return new ApplicationProcessor($inner, $security, $em);
    }
}
