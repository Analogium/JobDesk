package com.jobdesk.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limitation de débit en fenêtre glissante, en mémoire.
 *
 * <p>Le backend tourne en instance unique : un compteur en mémoire suffit et évite une
 * dépendance à Redis. Conséquence assumée : les compteurs repartent de zéro au
 * redémarrage, et cette protection ne tiendrait plus si l'app était répliquée.
 *
 * <p>Sert à empêcher l'envoi en boucle de mails de réinitialisation (qui consommerait le
 * quota Brevo) et le bourrinage de mots de passe.
 */
@Component
public class RateLimiter {

    /** Au-delà, on purge les compteurs expirés pour que la map ne grossisse pas sans fin. */
    private static final int CLEANUP_THRESHOLD = 1_000;

    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();

    /**
     * @return {@code true} si l'action est autorisée, {@code false} si le quota est atteint.
     */
    public boolean tryAcquire(String key, int max, Duration window) {
        Instant now = Instant.now();
        Instant cutoff = now.minus(window);

        if (hits.size() > CLEANUP_THRESHOLD) {
            purgeExpired(cutoff);
        }

        Deque<Instant> timestamps = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        // Verrou par clé : deux requêtes concurrentes sur la même clé ne doivent pas
        // pouvoir passer toutes les deux au moment où le quota est atteint.
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= max) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }

    /** Remet tous les compteurs à zéro. Utilisé par les tests, qui partagent le contexte Spring. */
    public void clear() {
        hits.clear();
    }

    private void purgeExpired(Instant cutoff) {
        hits.entrySet().removeIf(entry -> {
            Deque<Instant> timestamps = entry.getValue();
            synchronized (timestamps) {
                return timestamps.isEmpty() || timestamps.peekLast().isBefore(cutoff);
            }
        });
    }
}
