# Run using java and postgresql

Steps:

* Install [Postgresql 15](https://www.postgresql.org/download/)
* Create postgresql database with name 'molgenis' and with superadmin user/pass 'molgenis'. On Linux/Mac commandline:
    ```console
    psql postgres
    ```

    Or on macOS:

    ```
      psql -U postgres
    ```
* Then in psql console paste
    ```console
    create database molgenis;
    create user molgenis with login nosuperuser inherit createrole encrypted password 'molgenis';
    grant all privileges on database molgenis to molgenis;
    ```
* Java 21 required (e.g., [OpenJDK 21](https://adoptium.net/))
* Optionally, if you want to use [scripts](use_scripts_jobs.md) then also install python3
* Download molgenis-emx2-version-all.jar from [releases](https://github.com/molgenis/molgenis-emx2/releases).
* Start molgenis-emx2 using command below (will run on 8080)
    ```console
    java -jar molgenis-emx2-<version>-all.jar
    ```
* Open on http://localhost:8080

Optionally, you can change defaults using either java properties or using env variables:

* `MOLGENIS_POSTGRES_URI`
* `MOLGENIS_POSTGRES_USER`
* `MOLGENIS_POSTGRES_PASS`
* `MOLGENIS_HTTP_PORT`
* `MOLGENIS_ADMIN_PW`

For example:

```console
java -DMOLGENIS_POSTGRES_URI=jdbc:postgresql:mydatabase -DMOLGENIS_HTTP_PORT=9090 -jar molgenis-emx2-<version>-all.jar
```

# METRICS

MOLGENIS enables metrics api for example to use with prometheus. By default this is disabled. Please on use configure carefully to avoid abuse.

* `MOLGENIS_METRICS_ENABLED=true` enables the metrics on path /api/metrics
* `MOLGENIS_METRICS_PATH=/api/metric` enables the metrics path to be customized

For example:
java -DMOLGENIS_METRICS_ENABLED=true -jar molgenis-emx2-<version>-all.jar

# Tips

## On mac you can install postgres using [homebrew](https://formulae.brew.sh/formula/postgresql)

* to start/stop the service

```shell
brew services start postgresql
brew services stop postgresql
brew services restart postgresql
```

* to completely wipe postgresql:

```
brew services stop postgresql
rm -R /opt/homebrew/var/postgres
initdb -d  /opt/homebrew/var/postgres
brew services restart postgresql 
```

# FAQ

If you previously had an installation of Molgenis and want to start fresh, here is a .sql file which you can execute:

You can download the file here: [get clean permissions and db sql file](https://github.com/molgenis/molgenis-emx2/raw/master/docs/resources/clean-permissions-and-db.sql)

```
psql -U postgres -f clean-permissions-and-db.sql
```

You will be prompted to enter the password of the user postgres.

