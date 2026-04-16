<?php

use App\Kernel;

require_once __DIR__.'/../vendor/autoload.php';

$_SERVER['APP_ENV'] = 'test';
$_ENV['APP_ENV'] = 'test';
putenv('APP_ENV=test');

(new Symfony\Component\Dotenv\Dotenv())->bootEnv(__DIR__.'/../.env');

$kernel = new Kernel('test', true);
$kernel->boot();

return $kernel->getContainer()->get('doctrine')->getManager();
