package com.jobdesk.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Email invalide")
        String email,

        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 255, message = "Le nom est trop long")
        String name,

        // Plafond à 72 : BCrypt ignore silencieusement les octets au-delà, mieux vaut
        // refuser explicitement que d'accepter un mot de passe partiellement pris en compte.
        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, max = 72, message = "Le mot de passe doit faire entre 8 et 72 caractères")
        String password
) {
}
