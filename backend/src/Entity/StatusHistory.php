<?php

namespace App\Entity;

use App\Enum\ApplicationStatus;
use App\Repository\StatusHistoryRepository;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Serializer\Attribute\Groups;
use Symfony\Component\Uid\Uuid;

#[ORM\Entity(repositoryClass: StatusHistoryRepository::class)]
class StatusHistory
{
    #[ORM\Id]
    #[ORM\Column(type: 'uuid', unique: true)]
    #[ORM\GeneratedValue(strategy: 'CUSTOM')]
    #[ORM\CustomIdGenerator(class: 'doctrine.uuid_generator')]
    #[Groups(['application:read'])]
    private ?Uuid $id = null;

    #[ORM\ManyToOne(targetEntity: Application::class, inversedBy: 'statusHistories')]
    #[ORM\JoinColumn(nullable: false)]
    private ?Application $application = null;

    #[ORM\Column(enumType: ApplicationStatus::class, nullable: true)]
    #[Groups(['application:read'])]
    private ?ApplicationStatus $previousStatus = null;

    #[ORM\Column(enumType: ApplicationStatus::class)]
    #[Groups(['application:read'])]
    private ?ApplicationStatus $newStatus = null;

    #[ORM\Column]
    #[Groups(['application:read'])]
    private ?\DateTimeImmutable $changedAt = null;

    #[ORM\Column(length: 20)]
    #[Groups(['application:read'])]
    private string $trigger = 'manual'; // 'manual' | 'auto_mail'

    #[ORM\Column(type: Types::TEXT, nullable: true)]
    #[Groups(['application:read'])]
    private ?string $notes = null;

    public function __construct()
    {
        $this->changedAt = new \DateTimeImmutable();
    }

    public function getId(): ?Uuid { return $this->id; }

    public function getApplication(): ?Application { return $this->application; }
    public function setApplication(?Application $v): static { $this->application = $v; return $this; }

    public function getPreviousStatus(): ?ApplicationStatus { return $this->previousStatus; }
    public function setPreviousStatus(?ApplicationStatus $v): static { $this->previousStatus = $v; return $this; }

    public function getNewStatus(): ?ApplicationStatus { return $this->newStatus; }
    public function setNewStatus(ApplicationStatus $v): static { $this->newStatus = $v; return $this; }

    public function getChangedAt(): ?\DateTimeImmutable { return $this->changedAt; }

    public function getTrigger(): string { return $this->trigger; }
    public function setTrigger(string $v): static { $this->trigger = $v; return $this; }

    public function getNotes(): ?string { return $this->notes; }
    public function setNotes(?string $v): static { $this->notes = $v; return $this; }
}
