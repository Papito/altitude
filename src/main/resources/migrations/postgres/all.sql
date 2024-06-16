DROP SCHEMA IF EXISTS public CASCADE;
CREATE SCHEMA public;

CREATE TABLE _core (
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NULL
);

CREATE TABLE system (
  id INT NOT NULL DEFAULT 1,
  version INT NOT NULL,
  initialized BOOL NOT NULL DEFAULT FALSE
);
CREATE UNIQUE INDEX system_01 ON system(id);
INSERT INTO system(version, initialized) VALUES(1, False);

CREATE TABLE account(
  id CHAR(36) PRIMARY KEY
) INHERITS (_core);

CREATE TABLE repository(
  id CHAR(36) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  root_folder_id CHAR(36) NOT NULL,
  triage_folder_id CHAR(36) NOT NULL,
  file_store_type VARCHAR NOT NULL,
  file_store_config jsonb
) INHERITS (_core);

CREATE TABLE stats (
  repository_id CHAR(36) REFERENCES repository(id),
  dimension VARCHAR(60),
  dim_val INT NOT NULL DEFAULT 0
);
CREATE INDEX stats_01 ON stats(repository_id);
CREATE UNIQUE INDEX stats_02 ON stats(repository_id, dimension);

CREATE TABLE asset (
  id CHAR(36) PRIMARY KEY,
  repository_id CHAR(36) REFERENCES repository(id),
  user_id CHAR(36) REFERENCES account(id),
  checksum VARCHAR(64) NOT NULL,
  media_type VARCHAR(64) NOT NULL,
  media_subtype VARCHAR(64) NOT NULL,
  mime_type VARCHAR(64) NOT NULL,
  metadata jsonb,
  extracted_metadata jsonb,
  raw_metadata jsonb,
  folder_id CHAR(36) NOT NULL DEFAULT '1',
  filename TEXT NOT NULL,
  size_bytes INT NOT NULL,
  is_recycled INT NOT NULL DEFAULT 0
) INHERITS (_core);
CREATE UNIQUE INDEX asset_01 ON asset(repository_id, checksum, is_recycled);
CREATE UNIQUE INDEX asset_02 ON asset(repository_id, folder_id, filename, is_recycled);

CREATE TABLE metadata_field (
  id CHAR(36) PRIMARY KEY,
  repository_id CHAR(36) REFERENCES repository(id),
  name VARCHAR(255) NOT NULL,
  name_lc VARCHAR(255) NOT NULL,
  field_type VARCHAR(255) NOT NULL
) INHERITS (_core);
CREATE INDEX metadata_field_01 ON metadata_field(repository_id);
CREATE UNIQUE INDEX metadata_field_02 ON metadata_field(repository_id, name_lc);

CREATE TABLE folder (
  id CHAR(36) PRIMARY KEY,
  repository_id CHAR(36) REFERENCES repository(id),
  name VARCHAR(255) NOT NULL,
  name_lc VARCHAR(255) NOT NULL,
  parent_id CHAR(36) NOT NULL,
  num_of_assets INTEGER NOT NULL DEFAULT 0,
  is_recycled INT NOT NULL DEFAULT 0
) INHERITS (_core);
CREATE INDEX folder_01 ON folder(repository_id, parent_id);
CREATE UNIQUE INDEX folder_02 ON folder(repository_id, parent_id, name_lc);

CREATE TABLE search_parameter (
  repository_id CHAR(36) REFERENCES repository(id),
  asset_id CHAR(36) REFERENCES asset(id),
  field_id CHAR(36) REFERENCES metadata_field(id),
  field_value_kw TEXT NULL,
  field_value_num DECIMAL,
  field_value_bool BOOLEAN,
  field_value_dt TIMESTAMP WITH TIME ZONE
);
CREATE INDEX search_parameter_01 ON search_parameter(repository_id, field_id, field_value_kw);
CREATE INDEX search_parameter_02 ON search_parameter(repository_id, field_id, field_value_num);
CREATE INDEX search_parameter_03 ON search_parameter(repository_id, field_id, field_value_bool);
CREATE INDEX search_parameter_04 ON search_parameter(repository_id, field_id, field_value_dt);

CREATE TABLE search_document (
  repository_id CHAR(36) REFERENCES repository(id),
  asset_id CHAR(36) REFERENCES asset(id),
  metadata_values TEXT NOT NULL,
  body TEXT NOT NULL,
  tsv TSVECTOR NOT NULL
);
CREATE UNIQUE INDEX search_document_01 ON search_document(repository_id, asset_id);
CREATE INDEX search_document_02 ON search_document USING gin(tsv);

CREATE FUNCTION update_search_document_rank() RETURNS trigger AS $$
begin
new.tsv :=
setweight(to_tsvector('pg_catalog.english', new.metadata_values), 'A') ||
setweight(to_tsvector('pg_catalog.english', new.body), 'B');
return new;
end
$$ LANGUAGE plpgsql;

CREATE TRIGGER search_document_trigger BEFORE INSERT OR UPDATE
ON search_document
FOR EACH ROW
  EXECUTE PROCEDURE update_search_document_rank();
