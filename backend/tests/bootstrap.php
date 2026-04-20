<?php

use App\Doctrine\Type\EncryptedStringType;
use App\Kernel;
use Doctrine\ORM\Tools\SchemaTool;
use Symfony\Component\Dotenv\Dotenv;

require dirname(__DIR__).'/vendor/autoload.php';

// This bootstrap is only ever executed by PHPUnit, so we always force the
// test environment.  The Docker container has APP_ENV=dev in its OS env, and
// PHPUnit 13 applies <server> vars AFTER bootstrap runs, so we must set it
// here explicitly before Dotenv::bootEnv() resolves the environment.
$_SERVER['APP_ENV'] = 'test';
$_ENV['APP_ENV'] = 'test';
putenv('APP_ENV=test');

if (method_exists(Dotenv::class, 'bootEnv')) {
    (new Dotenv())->bootEnv(dirname(__DIR__).'/.env');
}

EncryptedStringType::setKey($_ENV['APP_SECRET'] ?? 'test-secret');

if ($_SERVER['APP_DEBUG']) {
    umask(0000);
}

// Build the SQLite test database from scratch so tests never need a real DB.
// Drop + recreate ensures the schema is always in sync with the current entities.
$kernel = new Kernel('test', true);
$kernel->boot();
$em = $kernel->getContainer()->get('doctrine.orm.entity_manager');
$schemaTool = new SchemaTool($em);
$metadata = $em->getMetadataFactory()->getAllMetadata();
$schemaTool->dropSchema($metadata);
$schemaTool->createSchema($metadata);
$kernel->shutdown();
