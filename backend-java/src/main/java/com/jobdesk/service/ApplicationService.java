package com.jobdesk.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.jobdesk.domain.Application;
import com.jobdesk.domain.ApplicationSource;
import com.jobdesk.domain.ApplicationStatus;
import com.jobdesk.domain.ContractType;
import com.jobdesk.domain.StatusHistory;
import com.jobdesk.domain.User;
import com.jobdesk.repository.ApplicationRepository;
import com.jobdesk.web.dto.ApplicationCreateRequest;
import com.jobdesk.web.dto.ApplicationDto;
import com.jobdesk.web.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ApplicationService {

    private static final int PAGE_SIZE = 30;
    private static final Set<String> SORTABLE = Set.of("appliedAt", "createdAt", "companyName");

    private final ApplicationRepository repository;

    public ApplicationService(ApplicationRepository repository) {
        this.repository = repository;
    }

    /**
     * Liste paginée des candidatures du user, filtrée/triée comme l'ancien API Platform.
     *
     * @param page    numéro de page 1-based (comme API Platform)
     * @param orders  map champ→sens ("asc"/"desc") issue des params order[...]
     * @param filters map champ→valeur (status, source, contractType)
     */
    @Transactional(readOnly = true)
    public PageResponse<ApplicationDto> list(User user, int page, Map<String, String> orders,
                                             Map<String, String> filters) {
        Sort sort = buildSort(orders);
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), PAGE_SIZE, sort);

        Specification<Application> spec = (root, query, cb) -> cb.equal(root.get("user"), user);
        spec = spec.and(filterSpec(filters));

        Page<Application> result = repository.findAll(spec, pageable);
        List<ApplicationDto> member = result.getContent().stream().map(ApplicationDto::from).toList();
        return new PageResponse<>(member, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ApplicationDto get(User user, UUID id) {
        return ApplicationDto.from(load(user, id));
    }

    @Transactional
    public ApplicationDto create(User user, ApplicationCreateRequest req) {
        Application app = new Application();
        app.setUser(user);
        app.setCompanyName(req.companyName());
        app.setJobTitle(req.jobTitle());
        app.setJobUrl(req.jobUrl());
        app.setJobDescription(req.jobDescription());
        app.setLocation(req.location());
        app.setContractType(req.contractType());
        app.setSalaryRange(req.salaryRange());
        if (req.status() != null) {
            app.setStatus(req.status());
        }
        app.setAppliedAt(parseDate(req.appliedAt()));
        if (req.source() != null) {
            app.setSource(req.source());
        }
        app.setNotes(req.notes());

        return ApplicationDto.from(repository.save(app));
    }

    /**
     * Applique un PATCH partiel (merge-patch) : seules les clés présentes dans le corps
     * sont modifiées. Crée une entrée d'historique si le statut change
     * (équivalent de l'ancien {@code ApplicationProcessor}).
     */
    @Transactional
    public ApplicationDto patch(User user, UUID id, JsonNode body) {
        Application app = load(user, id);
        ApplicationStatus previousStatus = app.getStatus();

        if (body.has("companyName")) {
            app.setCompanyName(text(body, "companyName"));
        }
        if (body.has("jobTitle")) {
            app.setJobTitle(text(body, "jobTitle"));
        }
        if (body.has("jobUrl")) {
            app.setJobUrl(text(body, "jobUrl"));
        }
        if (body.has("jobDescription")) {
            app.setJobDescription(text(body, "jobDescription"));
        }
        if (body.has("location")) {
            app.setLocation(text(body, "location"));
        }
        if (body.has("contractType")) {
            app.setContractType(enumValue(ContractType.class, text(body, "contractType")));
        }
        if (body.has("salaryRange")) {
            app.setSalaryRange(text(body, "salaryRange"));
        }
        if (body.has("status")) {
            String raw = text(body, "status");
            if (raw != null) {
                app.setStatus(enumValue(ApplicationStatus.class, raw));
            }
        }
        if (body.has("appliedAt")) {
            app.setAppliedAt(parseDate(text(body, "appliedAt")));
        }
        if (body.has("source")) {
            String raw = text(body, "source");
            if (raw != null) {
                app.setSource(ApplicationSource.fromValue(raw));
            }
        }
        if (body.has("notes")) {
            app.setNotes(text(body, "notes"));
        }

        if (previousStatus != app.getStatus()) {
            StatusHistory history = new StatusHistory();
            history.setPreviousStatus(previousStatus);
            history.setNewStatus(app.getStatus());
            history.setTrigger("manual");
            app.addStatusHistory(history);
        }

        return ApplicationDto.from(repository.save(app));
    }

    @Transactional
    public void delete(User user, UUID id) {
        repository.delete(load(user, id));
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private Application load(User user, UUID id) {
        return repository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Candidature introuvable"));
    }

    private Sort buildSort(Map<String, String> orders) {
        List<Sort.Order> sortOrders = new ArrayList<>();
        for (Map.Entry<String, String> e : orders.entrySet()) {
            if (SORTABLE.contains(e.getKey())) {
                Sort.Direction dir = "asc".equalsIgnoreCase(e.getValue())
                        ? Sort.Direction.ASC : Sort.Direction.DESC;
                sortOrders.add(new Sort.Order(dir, e.getKey()));
            }
        }
        return sortOrders.isEmpty() ? Sort.by(Sort.Direction.DESC, "createdAt") : Sort.by(sortOrders);
    }

    private Specification<Application> filterSpec(Map<String, String> filters) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (filters.containsKey("status")) {
                predicates.add(cb.equal(root.get("status"),
                        enumValue(ApplicationStatus.class, filters.get("status"))));
            }
            if (filters.containsKey("source")) {
                predicates.add(cb.equal(root.get("source"),
                        ApplicationSource.fromValue(filters.get("source"))));
            }
            if (filters.containsKey("contractType")) {
                predicates.add(cb.equal(root.get("contractType"),
                        enumValue(ContractType.class, filters.get("contractType"))));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private static String text(JsonNode body, String field) {
        JsonNode n = body.get(field);
        return (n == null || n.isNull()) ? null : n.asText();
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, raw);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Valeur invalide pour " + type.getSimpleName() + ": " + raw);
        }
    }

    /** Accepte une date seule ("2026-07-20", envoyée par input type=date) ou un datetime ISO. */
    private static LocalDateTime parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            if (raw.length() == 10) {
                return LocalDate.parse(raw).atStartOfDay();
            }
            return LocalDateTime.parse(raw);
        } catch (Exception e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Date invalide: " + raw);
        }
    }
}
