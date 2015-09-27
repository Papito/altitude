CREATE TABLE db_version(
  id varchar(24) PRIMARY KEY,
  version INT NOT NULL DEFAULT 0,
  migration_allowed INT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX db_version_idx ON db_version(version);

CREATE TABLE _core (
  created_at timestamp WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
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
  size_bytes INT NOT NULL
) INHERITS (_core);
CREATE UNIQUE INDEX asset_md5 ON asset(md5);

CREATE TABLE preview (
  id varchar(24) PRIMARY KEY,
  asset_id varchar(24),
  mime_type varchar(64) NOT NULL,
  data TEXT NOT NULL
) INHERITS (_core);


CREATE TABLE import_profile (
  id varchar(24) PRIMARY KEY,
  name varchar(255) NOT NULL,
  tag_data jsonb NOT NULL
) INHERITS (_core);

CREATE UNIQUE INDEX import_profile_name ON import_profile(name);

INSERT INTO db_version (id, version) VALUES(1, 1);