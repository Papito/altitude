# --- !Ups

CREATE TABLE asset (
    id varchar(24) NOT NULL,
    path varchar(1024) NOT NULL,
    media_type varchar(64) NOT NULL,
    media_subtype varchar(64) NOT NULL,
    mime_type varchar(64) NOT NULL,
    metadata jsonb,
    PRIMARY KEY (id)
);
 
# --- !Downs
 
DROP TABLE asset;
