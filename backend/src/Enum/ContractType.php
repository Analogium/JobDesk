<?php

namespace App\Enum;

enum ContractType: string
{
    case CDI = 'CDI';
    case CDD = 'CDD';
    case FREELANCE = 'FREELANCE';
    case INTERNSHIP = 'INTERNSHIP';
    case ALTERNANCE = 'ALTERNANCE';
    case OTHER = 'OTHER';
}
