<?php

namespace App\Doctrine\Type;

use Doctrine\DBAL\Platforms\AbstractPlatform;
use Doctrine\DBAL\Types\Type;
use LogicException;
use RuntimeException;

/**
 * Stores strings encrypted with AES-256-GCM in the database.
 * The key must be initialised once via setKey() before any DB operation.
 * Format on disk: base64(iv[12] . tag[16] . ciphertext).
 */
final class EncryptedStringType extends Type
{
    public const NAME = 'encrypted_string';
    private const CIPHER = 'aes-256-gcm';
    private const IV_LENGTH = 12;
    private const TAG_LENGTH = 16;

    private static string $key = '';

    public static function setKey(string $rawKey): void
    {
        // Derive a 32-byte key regardless of input length
        self::$key = hash('sha256', $rawKey, binary: true);
    }

    public function getName(): string
    {
        return self::NAME;
    }

    public function getSQLDeclaration(array $column, AbstractPlatform $platform): string
    {
        return $platform->getClobTypeDeclarationSQL($column);
    }

    public function convertToDatabaseValue(mixed $value, AbstractPlatform $platform): ?string
    {
        if (null === $value || '' === $value) {
            return null;
        }

        if ('' === self::$key) {
            throw new LogicException('EncryptedStringType: encryption key not set.');
        }

        $iv = random_bytes(self::IV_LENGTH);
        $tag = '';
        $cipher = openssl_encrypt((string) $value, self::CIPHER, self::$key, OPENSSL_RAW_DATA, $iv, $tag, '', self::TAG_LENGTH);

        if (false === $cipher) {
            throw new RuntimeException('Encryption failed.');
        }

        return base64_encode($iv.$tag.$cipher);
    }

    public function convertToPHPValue(mixed $value, AbstractPlatform $platform): ?string
    {
        if (null === $value || '' === $value) {
            return null;
        }

        if ('' === self::$key) {
            throw new LogicException('EncryptedStringType: encryption key not set.');
        }

        $decoded = base64_decode((string) $value, strict: true);
        if (false === $decoded) {
            return null;
        }

        $minLength = self::IV_LENGTH + self::TAG_LENGTH;
        if (mb_strlen($decoded, '8bit') <= $minLength) {
            return null;
        }

        $iv = mb_substr($decoded, 0, self::IV_LENGTH, '8bit');
        $tag = mb_substr($decoded, self::IV_LENGTH, self::TAG_LENGTH, '8bit');
        $cipher = mb_substr($decoded, $minLength, null, '8bit');

        $plain = openssl_decrypt($cipher, self::CIPHER, self::$key, OPENSSL_RAW_DATA, $iv, $tag);

        return false === $plain ? null : $plain;
    }

    public function requiresSQLCommentHint(AbstractPlatform $platform): bool
    {
        return true;
    }
}
