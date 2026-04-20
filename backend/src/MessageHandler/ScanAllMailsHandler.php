<?php

namespace App\MessageHandler;

use App\Message\ScanAllMailsMessage;
use App\Repository\UserRepository;
use App\Service\MailScanService;
use Symfony\Component\Messenger\Attribute\AsMessageHandler;

#[AsMessageHandler]
class ScanAllMailsHandler
{
    public function __construct(
        private readonly UserRepository $userRepository,
        private readonly MailScanService $mailScanService,
    ) {
    }

    public function __invoke(ScanAllMailsMessage $message): void
    {
        $users = $this->userRepository->findUsersWithGmail();

        foreach ($users as $user) {
            $this->mailScanService->scanForUser($user);
        }
    }
}
