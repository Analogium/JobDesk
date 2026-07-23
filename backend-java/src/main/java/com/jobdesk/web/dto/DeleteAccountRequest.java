package com.jobdesk.web.dto;

/**
 * Confirmation de suppression de compte. Le mot de passe n'est exigé que si le compte en
 * possède un : un compte créé via Google n'en a pas, l'action reste protégée par le JWT.
 */
public record DeleteAccountRequest(String password) {
}
