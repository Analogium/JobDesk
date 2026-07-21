package com.jobdesk.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Le lien de réinitialisation est incomplet")
        String token,

        // Mêmes bornes qu'à l'inscription (BCrypt ignore au-delà de 72 octets).
        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, max = 72, message = "Le mot de passe doit faire entre 8 et 72 caractères")
        String password
) {
}
