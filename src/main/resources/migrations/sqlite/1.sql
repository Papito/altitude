CREATE TABLE db_version(
  id varchar(24) PRIMARY KEY,
  version INT NOT NULL DEFAULT 0,
  migration_allowed INT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX db_version_idx ON db_version(version);

CREATE TABLE asset  (
  id varchar(24) PRIMARY KEY,
  md5 varchar(32) NOT NULL,
  media_type varchar(64) NOT NULL,
  media_subtype varchar(64) NOT NULL,
  mime_type varchar(64) NOT NULL,
  metadata TEXT,
  path TEXT NOT NULL,
  filename TEXT NOT NULL,
  size_bytes INT NOT NULL,
  created_at DATE DEFAULT (datetime('now', 'localtime')),
  updated_at timestamp DEFAULT NULL
);
CREATE UNIQUE INDEX asset_md5 ON asset(md5);
CREATE UNIQUE INDEX asset_path ON asset(path);

CREATE TABLE import_profile (
  id varchar(24) PRIMARY KEY,
  name varchar(255) NOT NULL,
  tag_data TEXT NOT NULL,
  created_at DATE DEFAULT (datetime('now', 'localtime')),
  updated_at timestamp WITHOUT TIME ZONE DEFAULT NULL
);

CREATE UNIQUE INDEX import_profile_name ON import_profile(name);

CREATE TABLE folder (
  id varchar(24) PRIMARY KEY,
  name varchar(255) NOT NULL,
  parent_id varchar(24) NOT NULL references folder(id),
  numOfAssets INTEGER NOT NULL DEFAULT 0,
  created_at DATE DEFAULT (datetime('now', 'localtime')),
  updated_at timestamp WITHOUT TIME ZONE DEFAULT NULL
);

CREATE UNIQUE INDEX folder_parent_id_and_name ON folder(parent_id, name);

INSERT INTO db_version (id, version) VALUES(1, 1);