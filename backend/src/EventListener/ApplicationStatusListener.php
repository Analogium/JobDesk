<?php

namespace App\EventListener;

use App\Entity\Application;
use App\Entity\StatusHistory;
use Doctrine\Bundle\DoctrineBundle\Attribute\AsEntityListener;
use Doctrine\ORM\Events;
use Doctrine\ORM\EntityManagerInterface;
use Doctrine\Persistence\Event\LifecycleEventArgs;

#[AsEntityListener(event: Events::preUpdate, entity: Application::class)]
class ApplicationStatusListener
{
    private array $originalData = [];

    public function preUpdate(Application $application, LifecycleEventArgs $args): void
    {
        $em = $args->getObjectManager();
        $uow = $em->getUnitOfWork();
        $changeSet = $uow->getEntityChangeSet($application);

        if (!isset($changeSet['status'])) {
            return;
        }

        [$previousStatus, $newStatus] = $changeSet['status'];

        if ($previousStatus === $newStatus) {
            return;
        }

        $history = new StatusHistory();
        $history->setApplication($application);
        $history->setPreviousStatus($previousStatus);
        $history->setNewStatus($newStatus);
        $history->setTrigger('manual');

        $em->persist($history);
    }
}
