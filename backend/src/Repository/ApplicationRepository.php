<?php

namespace App\Repository;

use App\Entity\Application;
use App\Entity\User;
use App\Enum\ApplicationStatus;
use DateTimeImmutable;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Application>
 */
class ApplicationRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Application::class);
    }

    /** @return Application[] */
    public function findByUserAndStatus(User $user, ApplicationStatus $status): array
    {
        return $this->createQueryBuilder('a')
            ->andWhere('a.user = :user')
            ->andWhere('a.status = :status')
            ->setParameter('user', $user)
            ->setParameter('status', $status)
            ->orderBy('a.appliedAt', 'DESC')
            ->getQuery()
            ->getResult();
    }

    /**
     * Returns applications that have been in APPLIED or WAITING status
     * for more than $days days without any status change.
     *
     * @return Application[]
     */
    public function findStaleApplications(User $user, int $days = 7): array
    {
        $threshold = new DateTimeImmutable("-{$days} days");

        return $this->createQueryBuilder('a')
            ->andWhere('a.user = :user')
            ->andWhere('a.status IN (:statuses)')
            ->andWhere('a.updatedAt < :threshold')
            ->setParameter('user', $user)
            ->setParameter('statuses', [ApplicationStatus::APPLIED, ApplicationStatus::WAITING])
            ->setParameter('threshold', $threshold)
            ->getQuery()
            ->getResult();
    }
}
