<?php

namespace App\EventSubscriber;

use App\Doctrine\Type\EncryptedStringType;
use Symfony\Component\Console\ConsoleEvents;
use Symfony\Component\EventDispatcher\EventSubscriberInterface;
use Symfony\Component\HttpKernel\KernelEvents;

final class EncryptionKeySubscriber implements EventSubscriberInterface
{
    public function __construct(private readonly string $encryptionKey)
    {
    }

    public static function getSubscribedEvents(): array
    {
        return [
            KernelEvents::REQUEST => ['initKey', 2048],
            ConsoleEvents::COMMAND => ['initKey', 2048],
        ];
    }

    public function initKey(): void
    {
        EncryptedStringType::setKey($this->encryptionKey);
    }
}
