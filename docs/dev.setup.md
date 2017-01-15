#### DEVELOPMENT SETUP

##### Core system requirements

* Java (8)
* Postgres database (9.4.4)

#### Database

If you have a fully-functioning setup, skip to setting up roles.
    
    sudo apt-get install libpq-dev
    sudo apt-get install libreadline-dev

    sudo adduser --shell /bin/bash postgres
    sudo mkdir -p /var/lib/pgsql/data
    sudo chown postgres /var/lib/pgsql/data
    sudo su - postgres
    /usr/local/pgsql/bin/initdb -D /var/lib/pgsql/data
    # in source
    cd contrib/start-scripts/
    
Path:

    PATH="/usr/local/pgsql/bin:$PATH"

Edit _linux_ and **update the file to set PGDATA variable to our data location**
    
    sudo cp linux /etc/init.d/postgresql
    sudo chmod 755 /etc/init.d/postgresql
    sudo /etc/init.d/postgresql start
    
For startup (on _Ubuntu 12.10+_):

    sudo apt-get install sysv-rc-conf
    sudo sysv-rc-conf postgresql on

###### DB Roles

And into the prompt to create some roles and users:
    
    sudo su - postgres
    psql
    
    psql $> CREATE ROLE andrei WITH LOGIN CREATEROLE CREATEDB;
    
Exit ...

    postgres=> CREATE USER altitude WITH PASSWORD 'dba';
    postgres=> CREATE USER "altitude-test" WITH PASSWORD 'dba';
    postgres=> create database "altitude";
    postgres=> create database "altitude-test";
    postgres=> GRANT ALL PRIVILEGES ON DATABASE "altitude" to "altitude";
    postgres=> GRANT ALL PRIVILEGES ON DATABASE "altitude-test" to "altitude-test";
