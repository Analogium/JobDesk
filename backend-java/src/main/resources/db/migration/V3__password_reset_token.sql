-- Réinitialisation de mot de passe par lien envoyé par mail.
--
-- On ne stocke JAMAIS le token en clair : seule son empreinte SHA-256 (64 caractères
-- hexadécimaux) est en base. Une fuite de la table ne permet donc pas de forger un lien
-- valide, exactement comme pour un mot de passe.
CREATE TABLE public.password_reset_token (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    token_hash character varying(64) NOT NULL,
    expires_at timestamp(0) without time zone NOT NULL,
    used_at timestamp(0) without time zone,
    created_at timestamp(0) without time zone NOT NULL
);

ALTER TABLE ONLY public.password_reset_token
    ADD CONSTRAINT password_reset_token_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.password_reset_token
    ADD CONSTRAINT password_reset_token_hash_key UNIQUE (token_hash);

-- ON DELETE CASCADE : supprimer un compte doit emporter ses liens de réinitialisation.
ALTER TABLE ONLY public.password_reset_token
    ADD CONSTRAINT password_reset_token_user_fk FOREIGN KEY (user_id)
    REFERENCES public."user"(id) ON DELETE CASCADE;

CREATE INDEX idx_password_reset_token_user ON public.password_reset_token (user_id);
