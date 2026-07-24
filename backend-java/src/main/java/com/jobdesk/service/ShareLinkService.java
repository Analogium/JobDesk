package com.jobdesk.service;

import com.jobdesk.domain.ShareLink;
import com.jobdesk.domain.User;
import com.jobdesk.repository.ApplicationRepository;
import com.jobdesk.repository.ShareLinkRepository;
import com.jobdesk.security.Tokens;
import com.jobdesk.web.dto.ShareLinkDto;
import com.jobdesk.web.dto.SharedApplicationDto;
import com.jobdesk.web.dto.SharedView;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Liens de partage en lecture seule : génération/révocation par le titulaire, et
 * résolution publique par un tiers.
 */
@Service
public class ShareLinkService {

    /** Borne haute de durée de validité (1 an), au-delà de laquelle « jamais » a plus de sens. */
    private static final int MAX_TTL_DAYS = 365;

    private final ShareLinkRepository shareLinkRepository;
    private final ApplicationRepository applicationRepository;

    public ShareLinkService(ShareLinkRepository shareLinkRepository,
                            ApplicationRepository applicationRepository) {
        this.shareLinkRepository = shareLinkRepository;
        this.applicationRepository = applicationRepository;
    }

    /**
     * Génère un lien pour le titulaire. Comme un seul lien est autorisé par compte, tout
     * lien existant est d'abord supprimé (l'ancienne URL cesse alors de fonctionner).
     *
     * @param ttlDays durée en jours, ou {@code null} pour un lien sans expiration.
     */
    @Transactional
    public ShareLinkDto createOrReplace(User user, Integer ttlDays) {
        if (ttlDays != null && (ttlDays < 1 || ttlDays > MAX_TTL_DAYS)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Durée de validité invalide.");
        }

        // Supprimer puis flush avant l'insert : la contrainte UNIQUE(user_id) interdit
        // deux liens, et Hibernate ordonne sinon l'INSERT avant le DELETE.
        shareLinkRepository.findByUser(user).ifPresent(shareLinkRepository::delete);
        shareLinkRepository.flush();

        ShareLink link = new ShareLink();
        link.setUser(user);
        link.setToken(Tokens.random());
        if (ttlDays != null) {
            link.setExpiresAt(LocalDateTime.now().plusDays(ttlDays));
        }
        return ShareLinkDto.from(shareLinkRepository.save(link));
    }

    /** Lien actif du titulaire, s'il en a un. */
    @Transactional(readOnly = true)
    public Optional<ShareLinkDto> current(User user) {
        return shareLinkRepository.findByUser(user).map(ShareLinkDto::from);
    }

    /** Révocation par le titulaire : l'URL partagée cesse de fonctionner. */
    @Transactional
    public void revoke(User user) {
        shareLinkRepository.deleteByUser(user);
    }

    /**
     * Résout un lien public en la vue partagée des candidatures du titulaire.
     *
     * <p>Un jeton inconnu <em>ou</em> expiré renvoie indistinctement 404 : on ne révèle
     * pas qu'un lien a existé.
     */
    @Transactional(readOnly = true)
    public SharedView resolve(String token) {
        ShareLink link = shareLinkRepository.findByToken(token)
                .filter(l -> l.isValid(LocalDateTime.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Lien de partage introuvable ou expiré."));

        User owner = link.getUser();
        List<SharedApplicationDto> applications = applicationRepository.findByUser(owner).stream()
                .map(SharedApplicationDto::from)
                .toList();

        return new SharedView(owner.getName(), link.getExpiresAt(), applications);
    }
}
