-- Refresh tokens : maintiennent la session active sans reconnexion, alors que
-- l'access token (JWT) reste volontairement court.
--
-- Seule l'empreinte SHA-256 du token est stockée (jamais le token) : une fuite de la
-- table ne permet pas de reprendre une session. Chaque refresh fait tourner le token
-- (l'ancien est révoqué, un nouveau est émis) → un token rejoué signale un vol.
CREATE TABLE public.refresh_token (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    token_hash character varying(64) NOT NULL,
    expires_at timestamp(0) without time zone NOT NULL,
    revoked_at timestamp(0) without time zone,
    created_at timestamp(0) without time zone NOT NULL
);

ALTER TABLE ONLY public.refresh_token
    ADD CONSTRAINT refresh_token_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.refresh_token
    ADD CONSTRAINT refresh_token_hash_key UNIQUE (token_hash);

-- ON DELETE CASCADE : supprimer un compte emporte ses sessions.
ALTER TABLE ONLY public.refresh_token
    ADD CONSTRAINT refresh_token_user_fk FOREIGN KEY (user_id)
    REFERENCES public."user"(id) ON DELETE CASCADE;

CREATE INDEX idx_refresh_token_user ON public.refresh_token (user_id);
