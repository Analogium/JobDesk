package com.jobdesk.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    private RateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new RateLimiter();
    }

    @Test
    void allowsUpToTheLimitThenRefuses() {
        Duration window = Duration.ofMinutes(1);

        assertThat(limiter.tryAcquire("k", 3, window)).isTrue();
        assertThat(limiter.tryAcquire("k", 3, window)).isTrue();
        assertThat(limiter.tryAcquire("k", 3, window)).isTrue();
        assertThat(limiter.tryAcquire("k", 3, window)).isFalse();
    }

    @Test
    void countsEachKeySeparately() {
        Duration window = Duration.ofMinutes(1);

        assertThat(limiter.tryAcquire("alice", 1, window)).isTrue();
        assertThat(limiter.tryAcquire("alice", 1, window)).isFalse();
        // Le quota d'un utilisateur ne doit pas bloquer les autres.
        assertThat(limiter.tryAcquire("bob", 1, window)).isTrue();
    }

    @Test
    void forgetsHitsOnceTheWindowHasPassed() throws Exception {
        Duration window = Duration.ofMillis(60);

        assertThat(limiter.tryAcquire("k", 1, window)).isTrue();
        assertThat(limiter.tryAcquire("k", 1, window)).isFalse();

        Thread.sleep(80);

        assertThat(limiter.tryAcquire("k", 1, window)).isTrue();
    }

    @Test
    void clearResetsEveryCounter() {
        Duration window = Duration.ofMinutes(1);

        assertThat(limiter.tryAcquire("k", 1, window)).isTrue();
        assertThat(limiter.tryAcquire("k", 1, window)).isFalse();

        limiter.clear();

        assertThat(limiter.tryAcquire("k", 1, window)).isTrue();
    }
}
