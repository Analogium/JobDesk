package com.jobdesk.web;

import com.jobdesk.domain.User;
import com.jobdesk.web.dto.UserDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Remplace l'ancien {@code MeProvider} (GET /api/me). */
@RestController
public class MeController {

    @GetMapping("/api/me")
    public UserDto me(@AuthenticationPrincipal User user) {
        return UserDto.from(user);
    }
}
