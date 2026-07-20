package com.jobdesk.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.jobdesk.domain.User;
import com.jobdesk.service.ApplicationService;
import com.jobdesk.web.dto.ApplicationCreateRequest;
import com.jobdesk.web.dto.ApplicationDto;
import com.jobdesk.web.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private static final Pattern ORDER_KEY = Pattern.compile("^order\\[(\\w+)\\]$");
    private static final Set<String> FILTER_KEYS = Set.of("status", "source", "contractType");

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<ApplicationDto> list(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam Map<String, String> allParams) {

        Map<String, String> orders = new HashMap<>();
        Map<String, String> filters = new HashMap<>();
        for (Map.Entry<String, String> e : allParams.entrySet()) {
            Matcher m = ORDER_KEY.matcher(e.getKey());
            if (m.matches()) {
                orders.put(m.group(1), e.getValue());
            } else if (FILTER_KEYS.contains(e.getKey())) {
                filters.put(e.getKey(), e.getValue());
            }
        }
        return service.list(user, page, orders, filters);
    }

    @GetMapping("/{id}")
    public ApplicationDto get(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        return service.get(user, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationDto create(@AuthenticationPrincipal User user,
                                 @Valid @RequestBody ApplicationCreateRequest req) {
        return service.create(user, req);
    }

    @PatchMapping("/{id}")
    public ApplicationDto patch(@AuthenticationPrincipal User user,
                                @PathVariable UUID id,
                                @RequestBody JsonNode body) {
        return service.patch(user, id, body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        service.delete(user, id);
        return ResponseEntity.noContent().build();
    }
}
