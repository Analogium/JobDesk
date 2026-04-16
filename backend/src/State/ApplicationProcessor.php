<?php

namespace App\State;

use ApiPlatform\Metadata\Operation;
use ApiPlatform\State\ProcessorInterface;
use App\Entity\Application;
use App\Entity\StatusHistory;
use App\Entity\User;
use App\Enum\ApplicationStatus;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\SecurityBundle\Security;

/**
 * @implements ProcessorInterface<Application, Application>
 */
class ApplicationProcessor implements ProcessorInterface
{
    public function __construct(
        private readonly ProcessorInterface $inner,
        private readonly Security $security,
        private readonly EntityManagerInterface $em,
    ) {
    }

    public function process(mixed $data, Operation $operation, array $uriVariables = [], array $context = []): mixed
    {
        if (!$data instanceof Application) {
            return $this->inner->process($data, $operation, $uriVariables, $context);
        }

        // Assigner l'utilisateur courant sur les nouvelles candidatures
        if (null === $data->getUser()) {
            $currentUser = $this->security->getUser();
            if ($currentUser instanceof User) {
                $data->setUser($currentUser);
            }
        }

        // Détecter un changement de statut (PATCH sur une entité existante)
        $previousStatus = null;
        if (null !== $data->getId()) {
            $originalData = $this->em->getUnitOfWork()->getOriginalEntityData($data);
            if (isset($originalData['status'])) {
                // Doctrine may return the raw string value or the enum object
                $orig = $originalData['status'];
                if (is_string($orig)) {
                    $orig = ApplicationStatus::from($orig);
                }
                if ($orig !== $data->getStatus()) {
                    $previousStatus = $orig;
                }
            }
        }

        $result = $this->inner->process($data, $operation, $uriVariables, $context);

        // Persister l'historique APRÈS le flush interne
        if (null !== $previousStatus && $result instanceof Application) {
            $history = new StatusHistory();
            $history->setApplication($result);
            $history->setPreviousStatus($previousStatus);
            $history->setNewStatus($result->getStatus());
            $history->setTrigger('manual');

            $this->em->persist($history);
            $this->em->flush();
        }

        return $result;
    }
}
