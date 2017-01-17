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
  metadata jsonb,
  extracted_metadata jsonb,
  raw_metadata jsonb,
  path TEXT NOT NULL,
  folder_id char(24) NOT NULL DEFAULT '1',
  filename TEXT NOT NULL,
  size_bytes INT NOT NULL,
  is_recycled BOOLEAN NOT NULL DEFAULT FALSE
) INHERITS (_core);
CREATE UNIQUE INDEX asset_01 ON asset(repository_id, md5, is_recycled);
CREATE UNIQUE INDEX asset_02 ON asset(repository_id, path, is_recycled);
CREATE INDEX asset_03 ON asset(repository_id, folder_id, is_recycled);

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

CREATE TABLE search_parameter (
  repository_id char(24) NOT NULL,
  asset_id char(24) NOT NULL,
  field_id char(24) NOT NULL,
  field_value_txt TEXT NOT NULL,
  field_value_kw TEXT NULL,
  field_value_num DECIMAL,
  field_value_bool BOOLEAN,
  field_value_dt TIMESTAMP WITH TIME ZONE
);
CREATE UNIQUE INDEX search_parameter_01 ON search_parameter(repository_id, asset_id, field_id, field_value_txt);
CREATE INDEX search_parameter_02 ON search_parameter(repository_id, field_id, field_value_txt);
CREATE INDEX search_parameter_03 ON search_parameter(repository_id, field_id, field_value_kw);
CREATE INDEX search_parameter_04 ON search_parameter(repository_id, field_id, field_value_num);
CREATE INDEX search_parameter_05 ON search_parameter(repository_id, field_id, field_value_bool);
CREATE INDEX search_parameter_06 ON search_parameter(repository_id, field_id, field_value_dt);

CREATE TABLE search_document (
  repository_id char(24) NOT NULL,
  asset_id char(24) NOT NULL,
  path TEXT NOT NULL,
  metadata_values TEXT,
  extracted_metadata_values TEXT,
  body TEXT,
  tsv TSVECTOR
);
CREATE UNIQUE INDEX search_document_01 ON search_document(repository_id, asset_id);

UPDATE search_document SET tsv = (
  setweight(to_tsvector(path), 'A') ||
  setweight(to_tsvector(metadata_values), 'B') ||
  setweight(to_tsvector(body), 'C') ||
  setweight(to_tsvector(extracted_metadata_values), 'D')
);

UPDATE search_document SET tsv = to_tsvector(
  'english', path || ' ' || metadata_values || ' ' || extracted_metadata_values || ' ' || body);

CREATE INDEX search_document_02 ON search_document USING gin(tsv);
