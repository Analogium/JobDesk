<?php

$finder = (new PhpCsFixer\Finder())
    ->in(__DIR__)
    ->exclude('var')
    ->notPath([
        'config/bundles.php',
        'config/reference.php',
    ])
;

return (new PhpCsFixer\Config())
    ->setRules([
        '@Symfony'             => true,
        'array_syntax'         => ['syntax' => 'short'],
        'ordered_imports'      => ['sort_algorithm' => 'alpha'],
        'no_unused_imports'    => true,
        'trailing_comma_in_multiline' => ['elements' => ['arrays', 'arguments', 'parameters']],
        'global_namespace_import' => ['import_classes' => true],
    ])
    ->setFinder($finder)
;
