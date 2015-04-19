# --- !Ups

--
-- TABLES
--

CREATE TABLE _core (
    created_at timestamp WITHOUT TIME ZONE NOT NULL,
    updated_at timestamp WITHOUT TIME ZONE DEFAULT NULL
);

CREATE TABLE asset (
    id varchar(24) NOT NULL,
    locations jsonb,
    media_type varchar(64) NOT NULL,
    media_subtype varchar(64) NOT NULL,
    mime_type varchar(64) NOT NULL,
    metadata jsonb,
    PRIMARY KEY (id)
) INHERITS (_core);

# --- !Downs

DROP TABLE IF EXISTS asset;
DROP TABLE IF EXISTS _core;