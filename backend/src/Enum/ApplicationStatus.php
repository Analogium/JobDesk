<?php

namespace App\Enum;

enum ApplicationStatus: string
{
    case DRAFT = 'DRAFT';
    case APPLIED = 'APPLIED';
    case WAITING = 'WAITING';
    case RELAUNCH = 'RELAUNCH';
    case INTERVIEW = 'INTERVIEW';
    case OFFER = 'OFFER';
    case REFUSED = 'REFUSED';
    case ABANDONED = 'ABANDONED';

    public function label(): string
    {
        return match($this) {
            self::DRAFT => 'À envoyer',
            self::APPLIED => 'Envoyée',
            self::WAITING => 'En attente',
            self::RELAUNCH => 'Relancé',
            self::INTERVIEW => 'Entretien',
            self::OFFER => 'Offre reçue',
            self::REFUSED => 'Refus',
            self::ABANDONED => 'Abandonné',
        };
    }

    public function isActive(): bool
    {
        return !in_array($this, [self::REFUSED, self::ABANDONED]);
    }
}
