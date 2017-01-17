CREATE TABLE system (
  id INT NOT NULL,
  version INT NOT NULL
);
CREATE UNIQUE INDEX system_01 ON system(id);
INSERT INTO system(id, version) VALUES(0, 0);

CREATE TABLE repository(
  id CHAR(24) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  root_folder_id CHAR(24) NOT NULL,
  uncat_folder_id CHAR(24) NOT NULL,
  created_at DATE DEFAULT (datetime('now', 'utc')),
  updated_at DATE DEFAULT NULL
);

CREATE TABLE stats (
  repository_id CHAR(24) NOT NULL,
  dimension VARCHAR(60) PRIMARY KEY,
  dim_val INT NOT NULL DEFAULT 0
);
CREATE INDEX stats_01 ON stats(repository_id);
CREATE UNIQUE INDEX stats_02 ON stats(repository_id, dimension);

CREATE TABLE asset  (
  id CHAR(24) PRIMARY KEY,
  repository_id CHAR(24) NOT NULL,
  user_id CHAR(24) NOT NULL,
  md5 VARCHAR(32) NOT NULL,
  media_type VARCHAR(64) NOT NULL,
  media_subtype VARCHAR(64) NOT NULL,
  mime_type VARCHAR(64) NOT NULL,
  extracted_metadata TEXT,
  raw_metadata TEXT,
  metadata TEXT,
  path TEXT NOT NULL,
  folder_id CHAR(24) NOT NULL,
  filename TEXT NOT NULL,
  size_bytes INT NOT NULL,
  is_recycled TINYINT NOT NULL DEFAULT FALSE,
  created_at DATE DEFAULT (datetime('now', 'utc')),
  updated_at DATE DEFAULT NULL
);
CREATE UNIQUE INDEX asset_01 ON asset(repository_id, md5);
CREATE UNIQUE INDEX asset_02 ON asset(repository_id, path);
CREATE INDEX asset_03 ON asset(repository_id, folder_id);

CREATE TABLE metadata_field (
  id CHAR(24) PRIMARY KEY,
  repository_id CHAR(24) NOT NULL,
  name VARCHAR(255) NOT NULL,
  name_lc VARCHAR(255) NOT NULL,
  field_type VARCHAR(255) NOT NULL,
  created_at DATE DEFAULT (datetime('now', 'utc')),
  updated_at DATE DEFAULT NULL
);
CREATE INDEX metadata_field_01 ON metadata_field(repository_id);
CREATE UNIQUE INDEX metadata_field_02 ON metadata_field(repository_id, name_lc);


CREATE TABLE folder (
  id CHAR(24) PRIMARY KEY,
  repository_id CHAR(24) NOT NULL,
  name VARCHAR(255) NOT NULL,
  name_lc VARCHAR(255) NOT NULL,
  parent_id CHAR(24) NOT NULL,
  num_of_assets INTEGER NOT NULL DEFAULT 0,
  created_at DATE DEFAULT (datetime('now', 'utc')),
  updated_at DATE DEFAULT NULL
);
CREATE INDEX folder_01 ON folder(repository_id, parent_id);
CREATE UNIQUE INDEX folder_02 ON folder(repository_id, parent_id, name_lc);

CREATE TABLE search_parameter (
  repository_id CHAR(24) NOT NULL,
  asset_id CHAR(24) NOT NULL,
  field_id CHAR(24) NOT NULL,
  field_value_txt TEXT NOT NULL,
  field_value_kw TEXT NULL,
  field_value_num DECIMAL,
  field_value_bool BOOLEAN,
  field_value_dt DATE
);
CREATE UNIQUE INDEX search_parameter_01 ON search_parameter(repository_id, asset_id, field_id, field_value_txt);
CREATE INDEX search_parameter_02 ON search_parameter(repository_id, field_id, field_value_txt);
CREATE INDEX search_parameter_03 ON search_parameter(repository_id, field_id, field_value_kw);
CREATE INDEX search_parameter_04 ON search_parameter(repository_id, field_id, field_value_num);
CREATE INDEX search_parameter_05 ON search_parameter(repository_id, field_id, field_value_bool);
CREATE INDEX search_parameter_06 ON search_parameter(repository_id, field_id, field_value_dt);

CREATE VIRTUAL TABLE search_document USING fts3(path, metadata_values, extracted_metadata_values, body);