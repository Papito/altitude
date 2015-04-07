# --- !Ups

CREATE TABLE asset (
    id varchar(24) NOT NULL,
    media_type varchar(64) NOT NULL,
    media_subtype varchar(64) NOT NULL,
    mime_type varchar(64) NOT NULL,
    PRIMARY KEY (id)
);
 
# --- !Downs
 
DROP TABLE asset;
