#!/bin/sh
set -e

# Generate JWT keys if missing
if [ ! -f config/jwt/private.pem ]; then
    mkdir -p config/jwt
    echo "Generating JWT keys..."
    openssl genpkey -algorithm RSA \
        -out config/jwt/private.pem \
        -pkeyopt rsa_keygen_bits:4096 \
        -pass pass:"${JWT_PASSPHRASE}" 2>/dev/null
    openssl rsa -pubout \
        -passin pass:"${JWT_PASSPHRASE}" \
        -in config/jwt/private.pem \
        -out config/jwt/public.pem 2>/dev/null
    echo "JWT keys generated."
fi

chown -R www-data:www-data config/jwt/

# Run migrations automatically in dev
if [ "${APP_ENV}" = "dev" ]; then
    php bin/console doctrine:migrations:migrate --no-interaction --allow-no-migration 2>/dev/null || true
fi

exec php-fpm
