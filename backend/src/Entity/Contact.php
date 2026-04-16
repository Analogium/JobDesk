<?php

namespace App\Entity;

use ApiPlatform\Metadata\ApiResource;
use ApiPlatform\Metadata\Delete;
use ApiPlatform\Metadata\GetCollection;
use ApiPlatform\Metadata\Patch;
use ApiPlatform\Metadata\Post;
use App\Repository\ContactRepository;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Serializer\Attribute\Groups;
use Symfony\Component\Uid\Uuid;
use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity(repositoryClass: ContactRepository::class)]
#[ApiResource(
    operations: [
        new GetCollection(uriTemplate: '/applications/{applicationId}/contacts'),
        new Post(uriTemplate: '/applications/{applicationId}/contacts'),
        new Patch(uriTemplate: '/applications/{applicationId}/contacts/{id}'),
        new Delete(uriTemplate: '/applications/{applicationId}/contacts/{id}'),
    ],
    normalizationContext: ['groups' => ['contact:read', 'application:read']],
    denormalizationContext: ['groups' => ['contact:write']],
    security: "is_granted('ROLE_USER')",
)]
class Contact
{
    #[ORM\Id]
    #[ORM\Column(type: 'uuid', unique: true)]
    #[ORM\GeneratedValue(strategy: 'CUSTOM')]
    #[ORM\CustomIdGenerator(class: 'doctrine.uuid_generator')]
    #[Groups(['application:read', 'contact:read'])]
    private ?Uuid $id = null;

    #[ORM\ManyToOne(targetEntity: Application::class, inversedBy: 'contacts')]
    #[ORM\JoinColumn(nullable: false)]
    private ?Application $application = null;

    #[ORM\Column(length: 255)]
    #[Assert\NotBlank]
    #[Groups(['application:read', 'contact:read', 'contact:write'])]
    private ?string $name = null;

    #[ORM\Column(length: 255, nullable: true)]
    #[Assert\Email]
    #[Groups(['application:read', 'contact:read', 'contact:write'])]
    private ?string $email = null;

    #[ORM\Column(length: 255, nullable: true)]
    #[Groups(['application:read', 'contact:read', 'contact:write'])]
    private ?string $role = null;

    #[ORM\Column(type: Types::TEXT, nullable: true)]
    #[Groups(['application:read', 'contact:read', 'contact:write'])]
    private ?string $notes = null;

    public function getId(): ?Uuid
    {
        return $this->id;
    }

    public function getApplication(): ?Application
    {
        return $this->application;
    }

    public function setApplication(?Application $v): static
    {
        $this->application = $v;

        return $this;
    }

    public function getName(): ?string
    {
        return $this->name;
    }

    public function setName(string $v): static
    {
        $this->name = $v;

        return $this;
    }

    public function getEmail(): ?string
    {
        return $this->email;
    }

    public function setEmail(?string $v): static
    {
        $this->email = $v;

        return $this;
    }

    public function getRole(): ?string
    {
        return $this->role;
    }

    public function setRole(?string $v): static
    {
        $this->role = $v;

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
}
