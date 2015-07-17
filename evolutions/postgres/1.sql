DROP SCHEMA IF EXISTS "altitude-test" CASCADE; CREATE SCHEMA "altitude-test";

CREATE TABLE _core (
  created_at timestamp WITHOUT TIME ZONE NOT NULL DEFAULT now(),
  updated_at timestamp WITHOUT TIME ZONE DEFAULT NULL
);

CREATE TABLE asset (
  id varchar(24) PRIMARY KEY,
  md5 varchar(32) NOT NULL,
  media_type varchar(64) NOT NULL,
  media_subtype varchar(64) NOT NULL,
  mime_type varchar(64) NOT NULL,
  metadata jsonb,
  path TEXT NOT NULL,
  filename TEXT NOT NULL,
  image_data BYTEA,
  size_bytes BIGINT NOT NULL
) INHERITS (_core);

CREATE TABLE preview (
  id varchar(24) PRIMARY KEY,
  asset_id varchar(24),
  mime_type varchar(64) NOT NULL,
  data TEXT NOT NULL
) INHERITS (_core);

CREATE UNIQUE INDEX asset_md5 ON asset(md5);