package com.jobdesk.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envoi des mails transactionnels via le relais SMTP (Brevo en production).
 *
 * <p>Quand aucun SMTP n'est configuré — développement local, tests — le mail n'est pas
 * envoyé : son contenu est journalisé. Le parcours reste donc utilisable sans dépendre
 * d'un service externe, et une panne SMTP ne fait jamais échouer la requête HTTP.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String host;
    private final String from;

    public MailService(ObjectProvider<JavaMailSender> mailSender,
                       @Value("${spring.mail.host:}") String host,
                       @Value("${app.mail.from:}") String from) {
        this.mailSender = mailSender;
        this.host = host;
        this.from = from;
    }

    public void sendPasswordReset(String to, String resetUrl) {
        String subject = "Réinitialisation de votre mot de passe JobDesk";
        String body = """
                Bonjour,

                Vous avez demandé à réinitialiser votre mot de passe JobDesk.
                Cliquez sur le lien ci-dessous pour en choisir un nouveau :

                %s

                Ce lien est valable une heure et ne peut servir qu'une fois.
                Si vous n'êtes pas à l'origine de cette demande, ignorez ce message :
                votre mot de passe actuel reste inchangé.

                — JobDesk
                """.formatted(resetUrl);

        send(to, subject, body);
    }

    private void send(String to, String subject, String body) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || host.isBlank() || from.isBlank()) {
            log.warn("SMTP non configuré : mail non envoyé à {}. Contenu :\n{}", to, body);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
            log.info("Mail « {} » envoyé à {}", subject, to);
        } catch (Exception e) {
            // Ne jamais propager : sinon l'échec révélerait à l'appelant que l'adresse
            // existe, et une panne Brevo renverrait une 500 au lieu d'un parcours normal.
            log.error("Échec de l'envoi du mail à {} : {}", to, e.getMessage());
        }
    }
}
