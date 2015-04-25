# --- !Ups

--
-- TABLES
--

CREATE TABLE _core (
    created_at timestamp WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at timestamp WITHOUT TIME ZONE DEFAULT NULL
);

CREATE TABLE asset (
    id varchar(24) PRIMARY KEY,
    media_type varchar(64) NOT NULL,
    media_subtype varchar(64) NOT NULL,
    mime_type varchar(64) NOT NULL,
    metadata jsonb,
    path TEXT NOT NULL
) INHERITS (_core);


# --- !Downs

DROP TABLE IF EXISTS asset;
DROP TABLE IF EXISTS _core;