-- Lien de partage en lecture seule : le titulaire génère un jeton qu'un tiers peut
-- utiliser pour consulter (jamais modifier) ses candidatures, dans une vue réduite
-- (sans les notes personnelles ni les contacts recruteurs).
--
-- Un seul lien actif par compte : la contrainte UNIQUE sur user_id fait respecter le
-- « un lien à la fois » côté base. En régénérer un remplace le précédent.
--
-- Le jeton est stocké en clair (et non haché comme les tokens de session) : il doit
-- pouvoir être réaffiché depuis les Paramètres pour être renvoyé au même destinataire.
-- Le périmètre est volontairement à faible sensibilité (lecture seule, données réduites).
CREATE TABLE public.share_link (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    token character varying(64) NOT NULL,
    created_at timestamp(0) without time zone NOT NULL,
    expires_at timestamp(0) without time zone
);

ALTER TABLE ONLY public.share_link
    ADD CONSTRAINT share_link_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.share_link
    ADD CONSTRAINT share_link_token_key UNIQUE (token);

-- Un lien maximum par compte.
ALTER TABLE ONLY public.share_link
    ADD CONSTRAINT share_link_user_key UNIQUE (user_id);

-- ON DELETE CASCADE : supprimer un compte emporte son lien de partage.
ALTER TABLE ONLY public.share_link
    ADD CONSTRAINT share_link_user_fk FOREIGN KEY (user_id)
    REFERENCES public."user"(id) ON DELETE CASCADE;
