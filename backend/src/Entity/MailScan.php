<?php

namespace App\Entity;

use App\Repository\MailScanRepository;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Uid\Uuid;

#[ORM\Entity(repositoryClass: MailScanRepository::class)]
class MailScan
{
    #[ORM\Id]
    #[ORM\Column(type: 'uuid', unique: true)]
    #[ORM\GeneratedValue(strategy: 'CUSTOM')]
    #[ORM\CustomIdGenerator(class: 'doctrine.uuid_generator')]
    private ?Uuid $id = null;

    #[ORM\ManyToOne(targetEntity: User::class)]
    #[ORM\JoinColumn(nullable: false)]
    private ?User $user = null;

    #[ORM\Column]
    private ?\DateTimeImmutable $scannedAt = null;

    #[ORM\Column(nullable: true)]
    private ?int $mailsAnalyzed = null;

    #[ORM\Column(nullable: true)]
    private ?int $matchesFound = null;

    #[ORM\Column(length: 20)]
    private string $status = 'success'; // 'success' | 'error'

    #[ORM\Column(type: Types::TEXT, nullable: true)]
    private ?string $errorMessage = null;

    public function __construct()
    {
        $this->scannedAt = new \DateTimeImmutable();
    }

    public function getId(): ?Uuid { return $this->id; }

    public function getUser(): ?User { return $this->user; }
    public function setUser(?User $v): static { $this->user = $v; return $this; }

    public function getScannedAt(): ?\DateTimeImmutable { return $this->scannedAt; }

    public function getMailsAnalyzed(): ?int { return $this->mailsAnalyzed; }
    public function setMailsAnalyzed(?int $v): static { $this->mailsAnalyzed = $v; return $this; }

    public function getMatchesFound(): ?int { return $this->matchesFound; }
    public function setMatchesFound(?int $v): static { $this->matchesFound = $v; return $this; }

    public function getStatus(): string { return $this->status; }
    public function setStatus(string $v): static { $this->status = $v; return $this; }

    public function getErrorMessage(): ?string { return $this->errorMessage; }
    public function setErrorMessage(?string $v): static { $this->errorMessage = $v; return $this; }
}
