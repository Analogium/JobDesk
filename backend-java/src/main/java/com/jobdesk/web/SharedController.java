package com.jobdesk.web;

import com.jobdesk.service.ShareLinkService;
import com.jobdesk.web.dto.SharedView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Accès public (sans authentification) aux candidatures partagées via un lien.
 * Lecture seule : aucun endpoint d'écriture n'existe ici.
 */
@RestController
public class SharedController {

    private final ShareLinkService shareLinkService;

    public SharedController(ShareLinkService shareLinkService) {
        this.shareLinkService = shareLinkService;
    }

    @GetMapping("/api/shared/{token}")
    public SharedView shared(@PathVariable String token) {
        return shareLinkService.resolve(token);
    }
}
