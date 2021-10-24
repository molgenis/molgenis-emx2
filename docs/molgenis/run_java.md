# Run using java and postgresql

Steps:

* Install [Postgresql](https://www.postgresql.org/download/) (we use 13)
* Create postgresql database with name 'molgenis' and with user/pass 'molgenis'. On Linux/Mac commandline:
    ```console
    psql postgres
    CREATE DATABASE molgenis;
    CREATE USER molgenis WITH LOGIN NOSUPERUSER INHERIT CREATEROLE encrypted password 'molgenis';
    GRANT ALL PRIVILEGES ON DATABASE molgenis TO molgenis;
    \c molgenis;
    CREATE EXTENSION IF NOT EXISTS pg_trgm;
    CREATE EXTENSION IF NOT EXISTS pgcrypto;
    ```
* Install java (we use adopt [OpenJDK 16](https://adoptopenjdk.net/))
* Download molgenis-emx2-version-all.jar from [releases](https://github.com/mswertz/molgenis-emx2/releases).
* Start molgenis-emx2 using command below (will run on 8080)
    ```console
    java -jar molgenis-emx2-<version>-all.jar
    ```
* Open on http://localhost:8080

Optionally, you can change defaults using either java properties or using env variables:

* MOLGENIS_POSTGRES_URI
* MOLGENIS_POSTGRES_USER
* MOLGENIS_POSTGRES_PASS
* MOLGENIS_HTTP_PORT
* MOLGENIS_ADMIN_PW

For example:

```console
java -DMOLGENIS_POSTGRES_URI=jdbc:postgresql:mydatabase -DMOLGENIS_HTTP_PORT=9090 -jar molgenis-emx2-<version>-all.jar
```

## Tips

On mac you can install postgres using homebrew

* completely wipe postgresql:

```
rm -R /opt/homebrew/var/postgres
initdb -d  /opt/homebrew/var/postgres
brew services restart postgresql 
```