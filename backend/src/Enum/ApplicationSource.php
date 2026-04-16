<?php

namespace App\Enum;

enum ApplicationSource: string
{
    case LINKEDIN = 'linkedin';
    case WTTJ = 'wttj';
    case INDEED = 'indeed';
    case MANUAL = 'manual';
    case OTHER = 'other';
}
