CREATE SCHEMA IF NOT EXISTS "altitude-test";

CREATE TABLE _core (
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NULL
);

CREATE TABLE system (
  id INT NOT NULL,
  version INT NOT NULL
);
CREATE UNIQUE INDEX system_record ON system(id);
INSERT INTO system(id, version) VALUES(0, 0);

CREATE TABLE repository(
  id char(24) PRIMARY KEY,
  name varchar(255) NOT NULL,
  description TEXT,
  root_folder_id char(24) NOT NULL,
  uncat_folder_id char(24) NOT NULL
) INHERITS (_core);

CREATE TABLE stats (
  repository_id char(24) NOT NULL,
  dimension varchar(60) PRIMARY KEY,
  dim_val INT NOT NULL DEFAULT 0
);
CREATE INDEX stats_01 ON stats(repository_id);
CREATE UNIQUE INDEX stats_02 ON stats(repository_id, dimension);

CREATE TABLE asset (
  id char(24) PRIMARY KEY,
  repository_id char(24) NOT NULL,
  user_id char(24) NOT NULL,
  md5 varchar(32) NOT NULL,
  media_type varchar(64) NOT NULL,
  media_subtype varchar(64) NOT NULL,
  mime_type varchar(64) NOT NULL,
  extracted_metadata jsonb,
  metadata jsonb,
  path TEXT NOT NULL,
  folder_id char(24) NOT NULL DEFAULT '1',
  filename TEXT NOT NULL,
  size_bytes INT NOT NULL
) INHERITS (_core);
CREATE UNIQUE INDEX asset_01 ON asset(repository_id, md5);
CREATE UNIQUE INDEX asset_02 ON asset(repository_id, path);
CREATE INDEX asset_03 ON asset(repository_id, folder_id);

CREATE TABLE trash (
  id char(24) PRIMARY KEY,
  repository_id char(24) NOT NULL,
  user_id char(24) NOT NULL,
  md5 varchar(32) NOT NULL,
  media_type varchar(64) NOT NULL,
  media_subtype varchar(64) NOT NULL,
  mime_type varchar(64) NOT NULL,
  extracted_metadata jsonb,
  metadata jsonb,
  path TEXT NOT NULL,
  folder_id char(24) NOT NULL,
  filename TEXT NOT NULL,
  size_bytes INT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NULL,
  recycled_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc')
);

CREATE TABLE metadata_field (
  id char(24) PRIMARY KEY,
  repository_id char(24) NOT NULL,
  name varchar(255) NOT NULL,
  name_lc varchar(255) NOT NULL,
  field_type varchar(255) NOT NULL
) INHERITS (_core);
CREATE INDEX metadata_field_01 ON metadata_field(repository_id);
CREATE UNIQUE INDEX metadata_field_02 ON metadata_field(repository_id, name_lc);

CREATE TABLE folder (
  id char(24) PRIMARY KEY,
  repository_id char(24) NOT NULL,
  name varchar(255) NOT NULL,
  name_lc varchar(255) NOT NULL,
  parent_id char(24) NOT NULL,
  num_of_assets INTEGER NOT NULL DEFAULT 0
) INHERITS (_core);
CREATE INDEX folder_01 ON folder(repository_id, parent_id);
CREATE UNIQUE INDEX folder_02 ON folder(repository_id, parent_id, name_lc);

CREATE TABLE search_token (
  repository_id char(24) NOT NULL,
  asset_id char(24) NOT NULL,
  field_id char(24) NOT NULL,
  field_value_txt TEXT NOT NULL,
  field_value_kw TEXT NULL,
  field_value_num DECIMAL,
  field_value_bool BOOLEAN,
  field_value_dt TIMESTAMP WITH TIME ZONE
);
CREATE UNIQUE INDEX search_token_01 ON search_token(repository_id, asset_id, field_id, field_value_txt);
CREATE INDEX search_token_02 ON search_token(repository_id, field_id, field_value_txt);
CREATE INDEX search_token_03 ON search_token(repository_id, field_id, field_value_kw);
CREATE INDEX search_token_04 ON search_token(repository_id, field_id, field_value_num);
CREATE INDEX search_token_05 ON search_token(repository_id, field_id, field_value_bool);
CREATE INDEX search_token_06 ON search_token(repository_id, field_id, field_value_dt);
