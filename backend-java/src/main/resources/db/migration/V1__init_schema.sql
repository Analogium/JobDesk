-- Schéma initial JobDesk (repris du schéma Doctrine existant).
-- Appliqué uniquement sur une base VIERGE (prod). Les bases déjà peuplées
-- sont baselinées à la version 1 (flyway.baseline-version=1) et sautent ce script.

CREATE TABLE public.application (
    id uuid NOT NULL,
    company_name character varying(255) NOT NULL,
    job_title character varying(255) NOT NULL,
    job_url character varying(2048) DEFAULT NULL::character varying,
    job_description text,
    location character varying(255) DEFAULT NULL::character varying,
    contract_type character varying(255) DEFAULT NULL::character varying,
    salary_range character varying(100) DEFAULT NULL::character varying,
    status character varying(255) NOT NULL,
    applied_at timestamp(0) without time zone DEFAULT NULL::timestamp without time zone,
    source character varying(255) NOT NULL,
    notes text,
    created_at timestamp(0) without time zone NOT NULL,
    updated_at timestamp(0) without time zone NOT NULL,
    user_id uuid NOT NULL
);
CREATE TABLE public.contact (
    id uuid NOT NULL,
    name character varying(255) NOT NULL,
    email character varying(255) DEFAULT NULL::character varying,
    role character varying(255) DEFAULT NULL::character varying,
    notes text,
    application_id uuid NOT NULL
);
CREATE TABLE public.mail_scan (
    id uuid NOT NULL,
    scanned_at timestamp(0) without time zone NOT NULL,
    mails_analyzed integer,
    matches_found integer,
    status character varying(20) NOT NULL,
    error_message text,
    user_id uuid NOT NULL
);
CREATE TABLE public.status_history (
    id uuid NOT NULL,
    previous_status character varying(255) DEFAULT NULL::character varying,
    new_status character varying(255) NOT NULL,
    changed_at timestamp(0) without time zone NOT NULL,
    trigger character varying(20) NOT NULL,
    notes text,
    application_id uuid NOT NULL
);
CREATE TABLE public."user" (
    id uuid NOT NULL,
    email character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    avatar_url character varying(500) DEFAULT NULL::character varying,
    google_token text,
    gmail_token text,
    gmail_refresh_token text,
    last_mail_scan_at timestamp(0) without time zone DEFAULT NULL::timestamp without time zone,
    created_at timestamp(0) without time zone NOT NULL,
    updated_at timestamp(0) without time zone NOT NULL
);
ALTER TABLE ONLY public.application
    ADD CONSTRAINT application_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.contact
    ADD CONSTRAINT contact_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.mail_scan
    ADD CONSTRAINT mail_scan_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.status_history
    ADD CONSTRAINT status_history_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public."user"
    ADD CONSTRAINT user_pkey PRIMARY KEY (id);
CREATE INDEX idx_2f6a07ce3e030acd ON public.status_history USING btree (application_id);
CREATE INDEX idx_4c62e6383e030acd ON public.contact USING btree (application_id);
CREATE INDEX idx_69c820c7a76ed395 ON public.mail_scan USING btree (user_id);
CREATE INDEX idx_a45bddc1a76ed395 ON public.application USING btree (user_id);
CREATE UNIQUE INDEX uniq_8d93d649e7927c74 ON public."user" USING btree (email);
ALTER TABLE ONLY public.status_history
    ADD CONSTRAINT fk_2f6a07ce3e030acd FOREIGN KEY (application_id) REFERENCES public.application(id);
ALTER TABLE ONLY public.contact
    ADD CONSTRAINT fk_4c62e6383e030acd FOREIGN KEY (application_id) REFERENCES public.application(id);
ALTER TABLE ONLY public.mail_scan
    ADD CONSTRAINT fk_69c820c7a76ed395 FOREIGN KEY (user_id) REFERENCES public."user"(id);
ALTER TABLE ONLY public.application
    ADD CONSTRAINT fk_a45bddc1a76ed395 FOREIGN KEY (user_id) REFERENCES public."user"(id);
