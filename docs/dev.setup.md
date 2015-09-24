### DEVELOPMENT SETUP

* Java (8)
* Postgres database (9.4.4)

###### DB Roles

And into the prompt to create some roles and users:
    
    sudo su - postgres
    psql
    
    postgres $> CREATE ROLE andrei WITH LOGIN CREATEROLE CREATEDB;
    
Exit ...

    postgres=> CREATE USER altitude WITH PASSWORD 'dba';
    postgres=> CREATE USER "altitude-test" WITH PASSWORD 'dba';
    postgres=> create database "altitude";
    postgres=> create database "altitude-test";
    postgres=> GRANT ALL PRIVILEGES ON DATABASE "altitude" to "altitude";
    postgres=> GRANT ALL PRIVILEGES ON DATABASE "altitude-test" to "altitude-test";
