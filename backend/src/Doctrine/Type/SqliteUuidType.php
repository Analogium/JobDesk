<?php

namespace App\Doctrine\Type;

use Doctrine\DBAL\Platforms\AbstractPlatform;
use Symfony\Bridge\Doctrine\Types\AbstractUidType;
use Symfony\Component\Uid\AbstractUid;
use Symfony\Component\Uid\Uuid;

/**
 * UUID type that stores as CHAR(36) string on all platforms.
 *
 * Registered in place of the standard binary UuidType during tests so that
 * SQLite (which lacks native UUID support and treats BINARY(16) as BLOB)
 * can perform equality comparisons correctly — PDO SQLite binds PHP strings
 * as TEXT, not BLOB, so BLOB = TEXT comparisons always return false.
 */
final class SqliteUuidType extends AbstractUidType
{
    public function getName(): string
    {
        return 'uuid';
    }

    protected function getUidClass(): string
    {
        return Uuid::class;
    }

    public function getSQLDeclaration(array $column, AbstractPlatform $platform): string
    {
        return 'CHAR(36)';
    }

    public function convertToDatabaseValue(mixed $value, AbstractPlatform $platform): ?string
    {
        if ($value instanceof AbstractUid) {
            return (string) $value;
        }

        if (null === $value || '' === $value) {
            return null;
        }

        return (string) $value;
    }
}
