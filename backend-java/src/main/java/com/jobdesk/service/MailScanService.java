package com.jobdesk.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.jobdesk.domain.Application;
import com.jobdesk.domain.ApplicationStatus;
import com.jobdesk.domain.MailScan;
import com.jobdesk.domain.StatusHistory;
import com.jobdesk.domain.User;
import com.jobdesk.repository.ApplicationRepository;
import com.jobdesk.repository.MailScanRepository;
import com.jobdesk.repository.UserRepository;
import com.jobdesk.security.GoogleOAuthService;
import com.jobdesk.web.dto.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Scan des mails Gmail pour détecter les changements de statut des candidatures.
 * Portage de l'ancien {@code App\Service\MailScanService} (Symfony).
 */
@Service
public class MailScanService {

    private static final Logger log = LoggerFactory.getLogger(MailScanService.class);

    private static final String GMAIL_API = "https://gmail.googleapis.com/gmail/v1";
    private static final int MAX_RESULTS = 100;
    private static final DateTimeFormatter GMAIL_DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private static final List<String> KEYWORDS_REFUSED = List.of(
            // FR
            "nous avons le regret", "avons le regret", "regret de vous informer",
            "n'avons pas retenu", "n'a pas été retenu", "pas été retenue",
            "ne pas retenir", "ne retenons pas", "ne donnons pas suite",
            "ne donner pas suite", "sans suite", "avons décidé de ne pas poursuivre",
            "ne correspond pas à", "votre profil ne correspond", "candidature n'a pas",
            "après étude de votre candidature", "poursuivre avec d'autres candidats",
            "nous ne sommes pas en mesure", "n'avons pas pu retenir", "refus",
            // EN
            "unfortunately", "regret to inform", "not moving forward",
            "not selected", "we will not be", "decided not to proceed",
            "not a match", "we have decided to", "we won't be moving",
            "other candidates", "position has been filled", "no longer considering",
            "not the right fit", "decided to move forward with other",
            "we regret", "not successful");

    private static final List<String> KEYWORDS_INTERVIEW = List.of(
            // FR
            "entretien", "rendez-vous", "visioconférence", "visio",
            "nous aimerions vous rencontrer", "souhaitons échanger avec vous",
            "disponibilités", "créneaux", "prise de contact", "appel téléphonique",
            "call téléphonique", "vous rencontrer", "échanger avec vous",
            "convocation", "invitation à un entretien",
            // EN
            "interview", "call with", "meeting with", "schedule a call",
            "schedule an interview", "phone screen", "phone call", "video call",
            "would like to meet", "set up a time", "find a time", "book a slot",
            "invite you to", "next steps",
            // Tools
            "teams meeting", "google meet", "zoom", "calendly");

    private static final List<String> KEYWORDS_OFFER = List.of(
            // FR
            "offre d'emploi", "proposition d'embauche", "nous vous proposons",
            "bienvenue dans", "nous souhaitons vous accueillir",
            "lettre d'engagement", "heureux de vous proposer",
            "ravis de vous accueillir", "rejoindre notre équipe",
            "offre de poste", "vous rejoindre",
            // EN
            "pleased to offer", "offer of employment", "welcome to the team",
            "job offer", "formal offer", "offer letter", "we would like to offer",
            "extend an offer", "happy to offer", "excited to offer",
            "we are delighted to offer");

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final MailScanRepository mailScanRepository;
    private final GoogleOAuthService google;
    private final RestClient http = RestClient.create();

    public MailScanService(ApplicationRepository applicationRepository,
                           UserRepository userRepository,
                           MailScanRepository mailScanRepository,
                           GoogleOAuthService google) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.mailScanRepository = mailScanRepository;
        this.google = google;
    }

    @Transactional
    public ScanResult scanForUser(User user) {
        String accessToken = refreshTokenIfNeeded(user);
        if (accessToken == null) {
            logScan(user, 0, 0, "error", "Impossible de rafraîchir le token Gmail");
            return ScanResult.error("Impossible de rafraîchir le token Gmail");
        }

        LocalDateTime since = user.getLastMailScanAt() != null
                ? user.getLastMailScanAt()
                : LocalDateTime.now().minusDays(7);

        List<Application> applications = applicationRepository.findByUser(user);
        if (applications.isEmpty()) {
            user.setLastMailScanAt(LocalDateTime.now());
            userRepository.save(user);
            return ScanResult.ok(0, 0);
        }

        List<String> messageIds;
        try {
            messageIds = fetchMessageIds(accessToken, since);
        } catch (Exception e) {
            logScan(user, 0, 0, "error", e.getMessage());
            return ScanResult.error(e.getMessage());
        }

        int matches = 0;
        for (String messageId : messageIds) {
            try {
                MessageDetail detail = fetchMessageDetail(accessToken, messageId);
                if (processMessage(detail, applications)) {
                    matches++;
                }
            } catch (Exception e) {
                // skip malformed messages
                log.debug("Message {} ignoré: {}", messageId, e.getMessage());
            }
        }

        user.setLastMailScanAt(LocalDateTime.now());
        applicationRepository.saveAll(applications);
        userRepository.save(user);
        logScan(user, messageIds.size(), matches, "success", null);
        return ScanResult.ok(messageIds.size(), matches);
    }

    // ─── Gmail API ──────────────────────────────────────────────────────────────

    private String refreshTokenIfNeeded(User user) {
        String refreshToken = user.getGmailRefreshToken();
        if (refreshToken == null) {
            return user.getGmailToken();
        }
        try {
            var refreshed = google.refreshAccessToken(refreshToken);
            if (refreshed != null && refreshed.accessToken() != null) {
                user.setGmailToken(refreshed.accessToken());
                userRepository.save(user);
                return refreshed.accessToken();
            }
        } catch (Exception e) {
            log.warn("Refresh du token Gmail échoué: {}", e.getMessage());
        }
        return user.getGmailToken();
    }

    private List<String> fetchMessageIds(String accessToken, LocalDateTime since) {
        String uri = UriComponentsBuilder.fromUriString(GMAIL_API + "/users/me/messages")
                .queryParam("q", "after:" + since.toLocalDate().format(GMAIL_DATE))
                .queryParam("maxResults", MAX_RESULTS)
                .encode().build().toUriString();

        JsonNode body = http.get().uri(uri)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .onStatus(status -> true, (req, res) -> { })
                .body(JsonNode.class);

        if (body == null) {
            return List.of();
        }
        if (body.has("error")) {
            throw new IllegalStateException("Gmail API: " + errorMessage(body.get("error")));
        }
        JsonNode messages = body.get("messages");
        if (messages == null || !messages.isArray()) {
            return List.of();
        }
        return messages.findValuesAsText("id");
    }

    private MessageDetail fetchMessageDetail(String accessToken, String messageId) {
        String uri = UriComponentsBuilder.fromUriString(GMAIL_API + "/users/me/messages/" + messageId)
                .queryParam("format", "metadata")
                .queryParam("metadataHeaders", "Subject")
                .queryParam("metadataHeaders", "From")
                .encode().build().toUriString();

        JsonNode body = http.get().uri(uri)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .onStatus(status -> true, (req, res) -> { })
                .body(JsonNode.class);

        if (body == null || body.has("error")) {
            throw new IllegalStateException("Gmail API (detail)");
        }

        String subject = "";
        String from = "";
        JsonNode headers = body.path("payload").path("headers");
        if (headers.isArray()) {
            for (JsonNode h : headers) {
                String name = h.path("name").asText();
                if ("Subject".equals(name)) {
                    subject = h.path("value").asText("");
                } else if ("From".equals(name)) {
                    from = h.path("value").asText("");
                }
            }
        }
        String snippet = body.path("snippet").asText("");
        return new MessageDetail(subject, from, snippet);
    }

    // ─── Matching ─────────────────────────────────────────────────────────────

    private boolean processMessage(MessageDetail message, List<Application> applications) {
        String subject = message.subject().toLowerCase();
        String from = message.from().toLowerCase();
        String snippet = message.snippet().toLowerCase();
        String fromDomain = extractDomain(from);

        for (Application application : applications) {
            if (!matchesApplication(application, subject, from, snippet, fromDomain)) {
                continue;
            }
            ApplicationStatus detected = detectStatus(subject + " " + snippet);
            if (detected == null || detected == application.getStatus()) {
                continue;
            }
            updateApplicationStatus(application, detected, message.subject());
            return true;
        }
        return false;
    }

    private boolean matchesApplication(Application application, String subject, String from,
                                       String snippet, String fromDomain) {
        String company = normalizeCompanyName(application.getCompanyName() == null
                ? "" : application.getCompanyName());
        if (company.isEmpty()) {
            return false;
        }
        if (!fromDomain.isEmpty() && fromDomain.contains(company)) {
            return true;
        }
        if (subject.contains(company) || from.contains(company) || snippet.contains(company)) {
            return true;
        }
        for (String word : company.split(" ")) {
            if (word.length() > 3
                    && (fromDomain.contains(word) || subject.contains(word) || snippet.contains(word))) {
                return true;
            }
        }
        return false;
    }

    static ApplicationStatus detectStatus(String text) {
        String lower = text.toLowerCase();
        for (String kw : KEYWORDS_REFUSED) {
            if (lower.contains(kw)) {
                return ApplicationStatus.REFUSED;
            }
        }
        for (String kw : KEYWORDS_OFFER) {
            if (lower.contains(kw)) {
                return ApplicationStatus.OFFER;
            }
        }
        for (String kw : KEYWORDS_INTERVIEW) {
            if (lower.contains(kw)) {
                return ApplicationStatus.INTERVIEW;
            }
        }
        return null;
    }

    private void updateApplicationStatus(Application application, ApplicationStatus newStatus,
                                         String subjectNote) {
        ApplicationStatus previous = application.getStatus();
        application.setStatus(newStatus);

        StatusHistory history = new StatusHistory();
        history.setPreviousStatus(previous);
        history.setNewStatus(newStatus);
        history.setTrigger("auto_mail");
        history.setNotes("Détecté depuis : " + subjectNote);
        application.addStatusHistory(history);
    }

    private void logScan(User user, int mailsAnalyzed, int matchesFound, String status, String errorMessage) {
        MailScan scan = new MailScan();
        scan.setUser(user);
        scan.setMailsAnalyzed(mailsAnalyzed);
        scan.setMatchesFound(matchesFound);
        scan.setStatus(status);
        scan.setErrorMessage(errorMessage);
        mailScanRepository.save(scan);
    }

    // ─── Utils ────────────────────────────────────────────────────────────────

    private static String extractDomain(String from) {
        String email;
        int lt = from.indexOf('<');
        int gt = from.indexOf('>');
        if (lt >= 0 && gt > lt) {
            email = from.substring(lt + 1, gt);
        } else if (from.contains("@")) {
            email = from.trim();
        } else {
            return "";
        }
        int at = email.lastIndexOf('@');
        return at >= 0 ? email.substring(at + 1).trim().toLowerCase() : "";
    }

    /** Équivalent de transliterator Any-Latin; Latin-ASCII + nettoyage des suffixes légaux. */
    static String normalizeCompanyName(String name) {
        String n = name.toLowerCase();
        n = Normalizer.normalize(n, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        n = n.replaceAll("\\b(sas|sarl|sa|sasu|ltd|inc|gmbh|bv|nv|llc|corp|group|groupe)\\b", "");
        n = n.replaceAll("[^a-z0-9 ]", "");
        return n.replaceAll("\\s+", " ").trim();
    }

    private static String errorMessage(JsonNode error) {
        if (error.has("message")) {
            return error.get("message").asText();
        }
        if (error.has("status")) {
            return error.get("status").asText();
        }
        return error.toString();
    }

    private record MessageDetail(String subject, String from, String snippet) {
    }
}
