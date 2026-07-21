-- Authentification email + mot de passe, en complément du login Google.
-- Nullable : les comptes créés via Google n'ont pas de mot de passe, ils
-- s'authentifient uniquement par OAuth. Un même compte (identifié par son
-- email) peut donc avoir l'un, l'autre, ou les deux.
ALTER TABLE public."user" ADD COLUMN password_hash character varying(255);
