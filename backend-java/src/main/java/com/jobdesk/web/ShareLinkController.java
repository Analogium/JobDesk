package com.jobdesk.web;

import com.jobdesk.domain.User;
import com.jobdesk.service.ShareLinkService;
import com.jobdesk.web.dto.CreateShareLinkRequest;
import com.jobdesk.web.dto.ShareLinkDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Gestion par le titulaire de son lien de partage en lecture seule (un seul à la fois). */
@RestController
public class ShareLinkController {

    private final ShareLinkService shareLinkService;

    public ShareLinkController(ShareLinkService shareLinkService) {
        this.shareLinkService = shareLinkService;
    }

    /** Génère (ou remplace) le lien de partage. */
    @PostMapping("/api/me/share-link")
    @ResponseStatus(HttpStatus.CREATED)
    public ShareLinkDto create(@AuthenticationPrincipal User user,
                               @RequestBody(required = false) CreateShareLinkRequest request) {
        Integer ttlDays = request != null ? request.ttlDays() : null;
        return shareLinkService.createOrReplace(user, ttlDays);
    }

    /** Lien actif du compte, ou 204 s'il n'y en a pas. */
    @GetMapping("/api/me/share-link")
    public ResponseEntity<ShareLinkDto> current(@AuthenticationPrincipal User user) {
        return shareLinkService.current(user)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** Révoque le lien : l'URL partagée cesse aussitôt de fonctionner. */
    @DeleteMapping("/api/me/share-link")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal User user) {
        shareLinkService.revoke(user);
    }
}
