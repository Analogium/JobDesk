package com.jobdesk.repository;

import com.jobdesk.domain.ShareLink;
import com.jobdesk.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ShareLinkRepository extends JpaRepository<ShareLink, UUID> {

    /** Lien courant d'un compte (au plus un, cf. contrainte UNIQUE sur user_id). */
    Optional<ShareLink> findByUser(User user);

    /**
     * Accès public par le jeton. {@code join fetch} sur le user : les candidatures du
     * titulaire sont chargées ensuite via ce user, qui ne doit donc pas rester en proxy
     * lazy hors transaction.
     */
    @Query("select s from ShareLink s join fetch s.user where s.token = :token")
    Optional<ShareLink> findByToken(String token);

    /** Suppression du compte, et révocation « un lien à la fois » avant régénération. */
    void deleteByUser(User user);

    /** Purge des liens dont l'échéance est dépassée (RGPD art. 5.1.e). */
    @Modifying
    @Query("delete from ShareLink s where s.expiresAt is not null and s.expiresAt < :now")
    int deleteExpired(LocalDateTime now);
}
