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
    metadata jsonb
) INHERITS (_core);

CREATE TABLE asset_storage (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    description TEXT
) INHERITS (_core);

INSERT INTO asset_storage (id, name, type) VALUES (1, 'My computer', 'LOCAL_FILE_SYSTEM');

CREATE TABLE asset_location (
    asset_id VARCHAR(24) REFERENCES asset(id),
    storage_id INTEGER REFERENCES asset_storage(id),
    path TEXT NOT NULL
) INHERITS (_core);


# --- !Downs

DROP TABLE IF EXISTS asset_location;
DROP TABLE IF EXISTS asset_storage;
DROP TABLE IF EXISTS asset;
DROP TABLE IF EXISTS _core;