CREATE TABLE system (
  id INT NOT NULL,
  version INT NOT NULL
);#END
CREATE UNIQUE INDEX system_01 ON system(id);#END
INSERT INTO system(id, version) VALUES(0, 0);#END

CREATE TABLE repository_user(
  id CHAR(36) PRIMARY KEY,
  created_at DATE DEFAULT (datetime('now', 'utc')),
  updated_at DATE DEFAULT NULL
);#END

CREATE TABLE repository(
  id CHAR(36) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  root_folder_id CHAR(36) NOT NULL,
  triage_folder_id CHAR(36) NOT NULL,
  file_store_type VARCHAR NOT NULL,
  file_store_config TEXT NOT NULL,
  created_at DATE DEFAULT (datetime('now', 'utc')),
  updated_at DATE DEFAULT NULL
);#END

CREATE TABLE stats (
  repository_id CHAR(36) NOT NULL,
  dimension VARCHAR(60),
  dim_val INT NOT NULL DEFAULT 0
);#END
CREATE INDEX stats_01 ON stats(repository_id);#END
CREATE UNIQUE INDEX stats_02 ON stats(repository_id, dimension);#END

CREATE TABLE asset  (
  id CHAR(36) PRIMARY KEY,
  repository_id CHAR(36) NOT NULL,
  user_id CHAR(36) NOT NULL,
  md5 VARCHAR(32) NOT NULL,
  media_type VARCHAR(64) NOT NULL,
  media_subtype VARCHAR(64) NOT NULL,
  mime_type VARCHAR(64) NOT NULL,
  extracted_metadata TEXT,
  raw_metadata TEXT,
  metadata TEXT,
  folder_id CHAR(36) NOT NULL,
  filename TEXT NOT NULL,
  size_bytes INT NOT NULL,
  is_recycled TINYINT NOT NULL DEFAULT 0,
  created_at DATE DEFAULT (datetime('now', 'utc')),
  updated_at DATE DEFAULT NULL
);#END
CREATE UNIQUE INDEX asset_01 ON asset(repository_id, md5, is_recycled);#END
CREATE UNIQUE INDEX asset_02 ON asset(repository_id, folder_id, filename, is_recycled);#END

CREATE TABLE metadata_field (
  id CHAR(36) PRIMARY KEY,
  repository_id CHAR(36) NOT NULL,
  name VARCHAR(255) NOT NULL,
  name_lc VARCHAR(255) NOT NULL,
  field_type VARCHAR(255) NOT NULL,
  created_at DATE DEFAULT (datetime('now', 'utc')),
  updated_at DATE DEFAULT NULL
);#END
CREATE INDEX metadata_field_01 ON metadata_field(repository_id);#END
CREATE UNIQUE INDEX metadata_field_02 ON metadata_field(repository_id, name_lc);#END


CREATE TABLE folder (
  id CHAR(36) PRIMARY KEY,
  repository_id CHAR(36) NOT NULL,
  name VARCHAR(255) NOT NULL,
  name_lc VARCHAR(255) NOT NULL,
  parent_id CHAR(36) NOT NULL,
  num_of_assets INTEGER NOT NULL DEFAULT 0,
  created_at DATE DEFAULT (datetime('now', 'utc')),
  updated_at DATE DEFAULT NULL
);#END
CREATE INDEX folder_01 ON folder(repository_id, parent_id);#END
CREATE UNIQUE INDEX folder_02 ON folder(repository_id, parent_id, name_lc);#END
CREATE UNIQUE INDEX folder_03 ON folder(repository_id, parent_id, name_lc);#END

CREATE TABLE search_parameter (
  repository_id CHAR(36) NOT NULL,
  asset_id CHAR(36) NOT NULL,
  field_id CHAR(36) NOT NULL,
  field_value_kw TEXT NULL,
  field_value_num DECIMAL,
  field_value_bool BOOLEAN,
  field_value_dt DATE
);#END
CREATE INDEX search_parameter_01 ON search_parameter(repository_id, field_id, field_value_kw);#END
CREATE INDEX search_parameter_02 ON search_parameter(repository_id, field_id, field_value_num);#END
CREATE INDEX search_parameter_03 ON search_parameter(repository_id, field_id, field_value_bool);#END
CREATE INDEX search_parameter_04 ON search_parameter(repository_id, field_id, field_value_dt);#END

CREATE VIRTUAL TABLE search_document USING fts4(repository_id, asset_id, body);#END
