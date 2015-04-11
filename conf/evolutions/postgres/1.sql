# --- !Ups

--
-- TABLES
--

CREATE TABLE _core (
    created_at timestamp without time zone NOT NULL default (now() at time zone 'utc'),
    updated_at timestamp without time zone
);

CREATE TABLE asset (
    id varchar(24) NOT NULL,
    storage jsonb,
    media_type varchar(64) NOT NULL,
    media_subtype varchar(64) NOT NULL,
    mime_type varchar(64) NOT NULL,
    metadata jsonb,
    PRIMARY KEY (id)
) INHERITS (_core);

CREATE OR REPLACE FUNCTION "update_json"(
    "json" json,
    "k"    TEXT,
    "v"    anyelement
) RETURNS json
LANGUAGE sql
IMMUTABLE
STRICT
AS $function$
SELECT CASE
       WHEN ("json" -> "k") IS NULL THEN "json"
       ELSE (SELECT concat('{', string_agg(to_json("key") || ':' || "value", ','), '}')
             FROM (SELECT *
                   FROM json_each("json")
                   WHERE "key" <> "k"
                   UNION ALL
                   SELECT "k", to_json("v")) AS "fields")::json
       END
$function$;


CREATE TYPE STORAGE_TYPE AS ENUM (
    'fs',
    'amazon_s3',
    'amazon_cloud_drvie',
    'google_drive'
);


CREATE TABLE storage (
    id SERIAL,
    name VARCHAR(100) NOT NULL,
    type STORAGE_TYPE NOT NULL,
    description TEXT,
    PRIMARY KEY (id)
) INHERITS (_core);

