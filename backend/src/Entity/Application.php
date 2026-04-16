<?php

namespace App\Entity;

use ApiPlatform\Doctrine\Orm\Filter\OrderFilter;
use ApiPlatform\Doctrine\Orm\Filter\SearchFilter;
use ApiPlatform\Metadata\ApiFilter;
use ApiPlatform\Metadata\ApiResource;
use ApiPlatform\Metadata\Delete;
use ApiPlatform\Metadata\Get;
use ApiPlatform\Metadata\GetCollection;
use ApiPlatform\Metadata\Patch;
use ApiPlatform\Metadata\Post;
use App\Enum\ApplicationSource;
use App\Enum\ApplicationStatus;
use App\Enum\ContractType;
use App\Repository\ApplicationRepository;
use App\State\ApplicationProcessor;
use DateTimeImmutable;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Serializer\Attribute\Groups;
use Symfony\Component\Uid\Uuid;
use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity(repositoryClass: ApplicationRepository::class)]
#[ORM\HasLifecycleCallbacks]
#[ApiResource(
    operations: [
        new GetCollection(),
        new Post(processor: ApplicationProcessor::class),
        new Get(),
        new Patch(processor: ApplicationProcessor::class),
        new Delete(),
    ],
    normalizationContext: ['groups' => ['application:read']],
    denormalizationContext: ['groups' => ['application:write']],
    security: "is_granted('ROLE_USER')",
)]
#[ApiFilter(SearchFilter::class, properties: ['status' => 'exact', 'source' => 'exact', 'contractType' => 'exact'])]
#[ApiFilter(OrderFilter::class, properties: ['appliedAt', 'createdAt', 'companyName'])]
class Application
{
    #[ORM\Id]
    #[ORM\Column(type: 'uuid', unique: true)]
    #[ORM\GeneratedValue(strategy: 'CUSTOM')]
    #[ORM\CustomIdGenerator(class: 'doctrine.uuid_generator')]
    #[Groups(['application:read'])]
    private ?Uuid $id = null;

    #[ORM\ManyToOne(targetEntity: User::class, inversedBy: 'applications')]
    #[ORM\JoinColumn(nullable: false)]
    private ?User $user = null;

    #[ORM\Column(length: 255)]
    #[Assert\NotBlank]
    #[Groups(['application:read', 'application:write'])]
    private ?string $companyName = null;

    #[ORM\Column(length: 255)]
    #[Assert\NotBlank]
    #[Groups(['application:read', 'application:write'])]
    private ?string $jobTitle = null;

    #[ORM\Column(length: 2048, nullable: true)]
    #[Groups(['application:read', 'application:write'])]
    private ?string $jobUrl = null;

    #[ORM\Column(type: Types::TEXT, nullable: true)]
    #[Groups(['application:read', 'application:write'])]
    private ?string $jobDescription = null;

    #[ORM\Column(length: 255, nullable: true)]
    #[Groups(['application:read', 'application:write'])]
    private ?string $location = null;

    #[ORM\Column(enumType: ContractType::class, nullable: true)]
    #[Groups(['application:read', 'application:write'])]
    private ?ContractType $contractType = null;

    #[ORM\Column(length: 100, nullable: true)]
    #[Groups(['application:read', 'application:write'])]
    private ?string $salaryRange = null;

    #[ORM\Column(enumType: ApplicationStatus::class)]
    #[Groups(['application:read', 'application:write'])]
    private ApplicationStatus $status = ApplicationStatus::DRAFT;

    #[ORM\Column(nullable: true)]
    #[Groups(['application:read', 'application:write'])]
    private ?DateTimeImmutable $appliedAt = null;

    #[ORM\Column(enumType: ApplicationSource::class)]
    #[Groups(['application:read', 'application:write'])]
    private ApplicationSource $source = ApplicationSource::MANUAL;

    #[ORM\Column(type: Types::TEXT, nullable: true)]
    #[Groups(['application:read', 'application:write'])]
    private ?string $notes = null;

    #[ORM\Column]
    #[Groups(['application:read'])]
    private ?DateTimeImmutable $createdAt = null;

    #[ORM\Column]
    #[Groups(['application:read'])]
    private ?DateTimeImmutable $updatedAt = null;

    /** @var Collection<int, StatusHistory> */
    #[ORM\OneToMany(targetEntity: StatusHistory::class, mappedBy: 'application', orphanRemoval: true)]
    #[ORM\OrderBy(['changedAt' => 'DESC'])]
    #[Groups(['application:read'])]
    private Collection $statusHistories;

    /** @var Collection<int, Contact> */
    #[ORM\OneToMany(targetEntity: Contact::class, mappedBy: 'application', orphanRemoval: true)]
    #[Groups(['application:read'])]
    private Collection $contacts;

    public function __construct()
    {
        $this->statusHistories = new ArrayCollection();
        $this->contacts = new ArrayCollection();
    }

    #[ORM\PrePersist]
    public function onPrePersist(): void
    {
        $this->createdAt = new DateTimeImmutable();
        $this->updatedAt = new DateTimeImmutable();
    }

    #[ORM\PreUpdate]
    public function onPreUpdate(): void
    {
        $this->updatedAt = new DateTimeImmutable();
    }

    public function getId(): ?Uuid
    {
        return $this->id;
    }

    public function getUser(): ?User
    {
        return $this->user;
    }

    public function setUser(?User $user): static
    {
        $this->user = $user;

        return $this;
    }

    public function getCompanyName(): ?string
    {
        return $this->companyName;
    }

    public function setCompanyName(string $v): static
    {
        $this->companyName = $v;

        return $this;
    }

    public function getJobTitle(): ?string
    {
        return $this->jobTitle;
    }

    public function setJobTitle(string $v): static
    {
        $this->jobTitle = $v;

        return $this;
    }

    public function getJobUrl(): ?string
    {
        return $this->jobUrl;
    }

    public function setJobUrl(?string $v): static
    {
        $this->jobUrl = $v;

        return $this;
    }

    public function getJobDescription(): ?string
    {
        return $this->jobDescription;
    }

    public function setJobDescription(?string $v): static
    {
        $this->jobDescription = $v;

        return $this;
    }

    public function getLocation(): ?string
    {
        return $this->location;
    }

    public function setLocation(?string $v): static
    {
        $this->location = $v;

        return $this;
    }

    public function getContractType(): ?ContractType
    {
        return $this->contractType;
    }

    public function setContractType(?ContractType $v): static
    {
        $this->contractType = $v;

        return $this;
    }

    public function getSalaryRange(): ?string
    {
        return $this->salaryRange;
    }

    public function setSalaryRange(?string $v): static
    {
        $this->salaryRange = $v;

        return $this;
    }

    public function getStatus(): ApplicationStatus
    {
        return $this->status;
    }

    public function setStatus(ApplicationStatus $v): static
    {
        $this->status = $v;

        return $this;
    }

    public function getAppliedAt(): ?DateTimeImmutable
    {
        return $this->appliedAt;
    }

    public function setAppliedAt(?DateTimeImmutable $v): static
    {
        $this->appliedAt = $v;

        return $this;
    }

    public function getSource(): ApplicationSource
    {
        return $this->source;
    }

    public function setSource(ApplicationSource $v): static
    {
        $this->source = $v;

        return $this;
    }

    public function getNotes(): ?string
    {
        return $this->notes;
    }

    public function setNotes(?string $v): static
    {
        $this->notes = $v;

        return $this;
    }

    public function getCreatedAt(): ?DateTimeImmutable
    {
        return $this->createdAt;
    }

    public function getUpdatedAt(): ?DateTimeImmutable
    {
        return $this->updatedAt;
    }

    /** @return Collection<int, StatusHistory> */
    public function getStatusHistories(): Collection
    {
        return $this->statusHistories;
    }

    /** @return Collection<int, Contact> */
    public function getContacts(): Collection
    {
        return $this->contacts;
    }
}
