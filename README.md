## Altitude

##### Database
    sudo apt-get install libpq-dev
    sudo apt-get install libreadline-dev

    # Install Postgres from source
    
    sudo adduser --shell /bin/bash postgres
    sudo mkdir -p /home/postgres/var/lib/data
    sudo chown postgres /home/postgres/var/lib/data
    sudo su - postgres
    /usr/local/pgsql/bin/initdb -D /home/postgres/var/lib/data
    # in source
    cd contrib/start-scripts/
    

Edit _linux_ and **update the file to set PGDATA variable to our data location**
    
    sudo cp linux /etc/init.d/postgresql
    sudo chmod 755 /etc/init.d/postgresql
    sudo /etc/init.d/postgresql start
    
For startup (on _Ubuntu 12.10+_):

    sudo apt-get install sysv-rc-conf
    sudo sysv-rc-conf postgresql on

Path:

    PATH="/usr/local/pgsql/bin:$PATH"

###### DB Roles

And into the prompt to create some roles and users:
    
    sudo su - postgres
    psql
    
    psql $> CREATE ROLE altitude WITH LOGIN CREATEDB;
    psql $> CREATE ROLE "altitude-test" WITH LOGIN CREATEDB;
    
Exit ...

    psql -Ualtitude postgres
    CREATE DATABASE altitude;

    psql -Ualtitude-test altitude-test
    CREATE DATABASE "altitude-test";
