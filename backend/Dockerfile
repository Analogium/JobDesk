FROM php:8.4-fpm-alpine AS base

RUN apk add --no-cache \
    git \
    curl \
    libpq-dev \
    icu-dev \
    libzip-dev \
    zip \
    unzip \
    openssl \
    && docker-php-ext-install \
        pdo \
        pdo_pgsql \
        intl \
        zip \
        opcache

COPY --from=composer:2 /usr/bin/composer /usr/bin/composer

WORKDIR /var/www/html

# ─── Dev ────────────────────────────────────────────────────────────────────
FROM base AS dev

COPY composer.json composer.lock* ./
RUN composer install --no-scripts --no-autoloader --prefer-dist

COPY . .
RUN composer dump-autoload --optimize

COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh && \
    mkdir -p config/jwt var/cache var/log && \
    chown -R www-data:www-data /var/www/html

EXPOSE 9000
ENTRYPOINT ["/entrypoint.sh"]

# ─── Prod ───────────────────────────────────────────────────────────────────
FROM base AS prod

ENV APP_ENV=prod APP_DEBUG=0

COPY composer.json composer.lock* ./
RUN composer install --no-dev --no-scripts --no-autoloader --prefer-dist --optimize-autoloader

COPY . .
RUN composer dump-autoload --optimize --no-dev

COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh && \
    mkdir -p config/jwt var/cache var/log && \
    php bin/console cache:warmup --env=prod && \
    chown -R www-data:www-data /var/www/html

EXPOSE 9000
ENTRYPOINT ["/entrypoint.sh"]
