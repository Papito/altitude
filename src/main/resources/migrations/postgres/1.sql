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

CREATE TABLE system (
  uncategorized_count INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE asset (
  id varchar(24) PRIMARY KEY,
  md5 varchar(32) NOT NULL,
  media_type varchar(64) NOT NULL,
  media_subtype varchar(64) NOT NULL,
  mime_type varchar(64) NOT NULL,
  metadata jsonb,
  path TEXT NOT NULL,
  folder_id varchar(24) NOT NULL DEFAULT '1',
  filename TEXT NOT NULL,
  size_bytes INT NOT NULL
) INHERITS (_core);
CREATE UNIQUE INDEX asset_md5 ON asset(md5);
CREATE UNIQUE INDEX asset_path ON asset(path);
CREATE INDEX asset_folder ON asset(folder_id);


CREATE TABLE import_profile (
  id varchar(24) PRIMARY KEY,
  name varchar(255) NOT NULL,
  tag_data jsonb NOT NULL
) INHERITS (_core);
CREATE UNIQUE INDEX import_profile_name ON import_profile(name);


CREATE TABLE folder (
  id varchar(24) PRIMARY KEY,
  name varchar(255) NOT NULL,
  name_lc varchar(255) NOT NULL,
  parent_id varchar(24) NOT NULL DEFAULT '0',
  num_of_assets INTEGER NOT NULL DEFAULT 0
) INHERITS (_core);
CREATE INDEX folder_parent_id ON folder(parent_id);
CREATE UNIQUE INDEX folder_parent_id_and_name ON folder(parent_id, name);


INSERT INTO db_version (id, version) VALUES(1, 1);