# --- !Ups

CREATE TABLE asset (
    id varchar(24) NOT NULL,
    PRIMARY KEY (id)
);
 
# --- !Downs
 
DROP TABLE asset;
