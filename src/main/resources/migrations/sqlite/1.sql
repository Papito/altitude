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
  uncat_folder_id char(24) NOT NULL,
  created_at DATE DEFAULT (datetime('now', 'utc')),
  updated_at DATE DEFAULT NULL
);

CREATE TABLE stats (
  repository_id char(24) NOT NULL,
  dimension varchar(60) PRIMARY KEY,
  dim_val INT NOT NULL DEFAULT 0
);
CREATE INDEX stats_repo ON stats(repository_id);
CREATE UNIQUE INDEX stats_repo_dimension ON stats(repository_id, dimension);

CREATE TABLE asset  (
  id char(24) PRIMARY KEY,
  repository_id char(24) NOT NULL,
  user_id char(24) NOT NULL,
  md5 varchar(32) NOT NULL,
  media_type varchar(64) NOT NULL,
  media_subtype varchar(64) NOT NULL,
  mime_type varchar(64) NOT NULL,
  extracted_metadata TEXT,
  metadata TEXT,
  path TEXT NOT NULL,
  folder_id char(24) NOT NULL,
  filename TEXT NOT NULL,
  size_bytes INT NOT NULL,
  created_at DATE DEFAULT (datetime('now', 'utc')),
  updated_at DATE DEFAULT NULL
);
CREATE UNIQUE INDEX asset_md5 ON asset(repository_id, md5);
CREATE UNIQUE INDEX asset_path ON asset(repository_id, path);
CREATE INDEX asset_folder ON asset(repository_id, folder_id);

CREATE TABLE trash  (
  id char(24) PRIMARY KEY,
  repository_id char(24) NOT NULL,
  user_id char(24) NOT NULL,
  md5 varchar(32) NOT NULL,
  media_type varchar(64) NOT NULL,
  media_subtype varchar(64) NOT NULL,
  mime_type varchar(64) NOT NULL,
  extracted_metadata TEXT,
  metadata TEXT,
  path TEXT NOT NULL,
  folder_id char(24) NOT NULL,
  filename TEXT NOT NULL,
  size_bytes INT NOT NULL,
  created_at DATE NOT NULL,
  updated_at DATE DEFAULT NULL,
  recycled_at DATE DEFAULT (datetime('now', 'utc'))
);

CREATE TABLE metadata_field (
  id char(24) PRIMARY KEY,
  repository_id char(24) NOT NULL,
  name varchar(255) NOT NULL,
  name_lc varchar(255) NOT NULL,
  field_type varchar(255) NOT NULL,
  created_at DATE DEFAULT (datetime('now', 'utc')),
  updated_at DATE DEFAULT NULL
);
CREATE INDEX metadata_field_repo ON metadata_field(repository_id);
CREATE UNIQUE INDEX metadata_field_name ON metadata_field(repository_id, name_lc);

CREATE TABLE metadata_value (
  repository_id char(24) NOT NULL,
  asset_id char(24) NOT NULL,
  field_id char(24) NOT NULL,
  field_value TEXT NOT NULL,
  field_value_lc TEXT NOT NULL,
  created_at DATE DEFAULT (datetime('now', 'utc')),
  updated_at DATE DEFAULT NULL
);
CREATE INDEX metadata_value_id_asset_id ON metadata_value(repository_id, asset_id, field_id);
CREATE UNIQUE INDEX metadata_value_field_and_value ON metadata_value(repository_id, asset_id, field_id, field_value_lc);

--CREATE TABLE import_profile (
--  id char(24) PRIMARY KEY,
--  name varchar(255) NOT NULL,
--  tag_data TEXT NOT NULL,
--  created_at DATE DEFAULT (datetime('now', 'localtime')),
--  updated_at DATE DEFAULT NULL
--);
--CREATE UNIQUE INDEX import_profile_name ON import_profile(name);


CREATE TABLE folder (
  id char(24) PRIMARY KEY,
  repository_id char(24) NOT NULL,
  name varchar(255) NOT NULL,
  name_lc varchar(255) NOT NULL,
  parent_id char(24) NOT NULL,
  num_of_assets INTEGER NOT NULL DEFAULT 0,
  created_at DATE DEFAULT (datetime('now', 'utc')),
  updated_at DATE DEFAULT NULL
);
CREATE INDEX folder_parent_id ON folder(repository_id, parent_id);
CREATE UNIQUE INDEX folder_parent_id_and_name ON folder(repository_id, parent_id, name_lc);
