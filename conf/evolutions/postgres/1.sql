# --- !Ups

--
-- TABLES
--

CREATE TABLE _core (
    created_at timestamp without time zone NOT NULL default (now() at time zone 'utc'),
    updated_at timestamp without time zone
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

DROP TABLE asset;
DROP TABLE _core;