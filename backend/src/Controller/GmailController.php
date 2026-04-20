<?php

namespace App\Controller;

use App\Entity\User;
use App\Repository\UserRepository;
use App\Service\MailScanService;
use DateTimeInterface;
use Doctrine\ORM\EntityManagerInterface;
use League\OAuth2\Client\Provider\Google;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Bundle\SecurityBundle\Security;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\RedirectResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Throwable;

#[Route('/api/gmail')]
class GmailController extends AbstractController
{
    public function __construct(
        private readonly EntityManagerInterface $em,
        private readonly Security $security,
        private readonly UserRepository $userRepository,
        private readonly MailScanService $mailScanService,
        private readonly string $appSecret,
        private readonly string $googleClientId,
        private readonly string $googleClientSecret,
        private readonly string $backendUrl,
        private readonly string $frontendUrl,
    ) {
    }

    #[Route('/connect', name: 'gmail_connect', methods: ['GET'])]
    public function connect(): JsonResponse
    {
        /** @var User $user */
        $user = $this->security->getUser();

        $provider = $this->createGoogleProvider();
        $state = $this->signState((string) $user->getId());

        $authUrl = $provider->getAuthorizationUrl([
            'scope' => ['https://www.googleapis.com/auth/gmail.readonly'],
            'access_type' => 'offline',
            'prompt' => 'consent',
            'state' => $state,
        ]);

        return new JsonResponse(['url' => $authUrl]);
    }

    #[Route('/callback', name: 'gmail_callback', methods: ['GET'])]
    public function callback(Request $request): RedirectResponse
    {
        $code = $request->query->get('code');
        $state = $request->query->get('state');
        $error = $request->query->get('error');

        if ($error || !$code || !$state) {
            return new RedirectResponse($this->frontendUrl.'/settings?gmail=error');
        }

        $userId = $this->verifyState((string) $state);
        if (!$userId) {
            return new RedirectResponse($this->frontendUrl.'/settings?gmail=error');
        }

        $user = $this->userRepository->find($userId);
        if (!$user instanceof User) {
            return new RedirectResponse($this->frontendUrl.'/settings?gmail=error');
        }

        try {
            $provider = $this->createGoogleProvider();
            $token = $provider->getAccessToken('authorization_code', ['code' => $code]);

            $user->setGmailToken($token->getToken());
            $user->setGmailRefreshToken($token->getRefreshToken() ?? $user->getGmailRefreshToken());
            $this->em->flush();
        } catch (Throwable) {
            return new RedirectResponse($this->frontendUrl.'/settings?gmail=error');
        }

        return new RedirectResponse($this->frontendUrl.'/settings?gmail=connected');
    }

    #[Route('/status', name: 'gmail_status', methods: ['GET'])]
    public function status(): JsonResponse
    {
        /** @var User $user */
        $user = $this->security->getUser();

        return new JsonResponse([
            'connected' => null !== $user->getGmailToken(),
            'lastMailScanAt' => $user->getLastMailScanAt()?->format(DateTimeInterface::ATOM),
        ]);
    }

    #[Route('/disconnect', name: 'gmail_disconnect', methods: ['DELETE'])]
    public function disconnect(): JsonResponse
    {
        /** @var User $user */
        $user = $this->security->getUser();

        $user->setGmailToken(null);
        $user->setGmailRefreshToken(null);
        $this->em->flush();

        return new JsonResponse(null, Response::HTTP_NO_CONTENT);
    }

    #[Route('/scan', name: 'gmail_scan', methods: ['POST'])]
    public function scan(): JsonResponse
    {
        /** @var User $user */
        $user = $this->security->getUser();

        if (null === $user->getGmailToken()) {
            return new JsonResponse(['error' => 'Gmail non connecté'], Response::HTTP_BAD_REQUEST);
        }

        $result = $this->mailScanService->scanForUser($user);

        return new JsonResponse($result);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private function createGoogleProvider(): Google
    {
        return new Google([
            'clientId' => $this->googleClientId,
            'clientSecret' => $this->googleClientSecret,
            'redirectUri' => $this->backendUrl.'/api/gmail/callback',
        ]);
    }

    private function signState(string $userId): string
    {
        $payload = base64_encode($userId);
        $sig = hash_hmac('sha256', $payload, $this->appSecret);

        return $payload.'.'.$sig;
    }

    private function verifyState(string $state): ?string
    {
        $parts = explode('.', $state, 2);
        if (2 !== count($parts)) {
            return null;
        }

        [$payload, $sig] = $parts;
        $expected = hash_hmac('sha256', $payload, $this->appSecret);

        if (!hash_equals($expected, $sig)) {
            return null;
        }

        return base64_decode($payload) ?: null;
    }
}
