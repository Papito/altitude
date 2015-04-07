# --- !Ups

CREATE TABLE asset (
    id varchar(24) NOT NULL,
    media_type varchar(255) NOT NULL,
    media_subtype varchar(255) NOT NULL,
    PRIMARY KEY (id)
);
 
# --- !Downs
 
DROP TABLE asset;
