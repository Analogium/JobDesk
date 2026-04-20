<?php

namespace App\Repository;

use App\Entity\User;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<User>
 */
class UserRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, User::class);
    }

    /** @return User[] */
    public function findUsersWithGmail(): array
    {
        return $this->createQueryBuilder('u')
            ->where('u.gmailToken IS NOT NULL')
            ->andWhere('u.gmailRefreshToken IS NOT NULL')
            ->getQuery()
            ->getResult();
    }
}
