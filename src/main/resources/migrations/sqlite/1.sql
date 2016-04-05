CREATE TABLE system (
  id INT NOT NULL,
  version INT NOT NULL
);
CREATE UNIQUE INDEX system_record ON system(id);
INSERT INTO system(id, version) VALUES(0, 0);


CREATE TABLE asset  (
  id varchar(24) PRIMARY KEY,
  md5 varchar(32) NOT NULL,
  media_type varchar(64) NOT NULL,
  media_subtype varchar(64) NOT NULL,
  mime_type varchar(64) NOT NULL,
  metadata TEXT,
  path TEXT NOT NULL,
  folder_id varchar(24) NOT NULL DEFAULT "1",
  filename TEXT NOT NULL,
  size_bytes INT NOT NULL,
  created_at DATE DEFAULT (datetime('now', 'localtime')),
  updated_at DATE DEFAULT NULL
);
CREATE UNIQUE INDEX asset_md5 ON asset(md5);
CREATE UNIQUE INDEX asset_path ON asset(path);
CREATE INDEX asset_folder ON asset(folder_id);


--CREATE TABLE import_profile (
--  id varchar(24) PRIMARY KEY,
--  name varchar(255) NOT NULL,
--  tag_data TEXT NOT NULL,
--  created_at DATE DEFAULT (datetime('now', 'localtime')),
--  updated_at DATE DEFAULT NULL
--);
--CREATE UNIQUE INDEX import_profile_name ON import_profile(name);


CREATE TABLE folder (
  id varchar(24) PRIMARY KEY,
  name varchar(255) NOT NULL,
  name_lc varchar(255) NOT NULL,
  parent_id varchar(24) NOT NULL DEFAULT "0",
  num_of_assets INTEGER NOT NULL DEFAULT 0,
  created_at DATE DEFAULT (datetime('now', 'localtime')),
  updated_at DATE DEFAULT NULL
);
CREATE INDEX folder_parent_id ON folder(parent_id);
CREATE UNIQUE INDEX folder_parent_id_and_name ON folder(parent_id, name);
