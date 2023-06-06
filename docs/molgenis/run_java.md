# Run using java and postgresql

Steps:

* Install [Postgresql](https://www.postgresql.org/download/) (we use 14)
* Create postgresql database with name 'molgenis' and with superadmin user/pass 'molgenis'. On Linux/Mac commandline:
    ```console
    psql postgres
    ```
* Then in psql console paste
    ```console
    create database molgenis;
    create user molgenis with login nosuperuser inherit createrole encrypted password 'molgenis';
    grant all privileges on database molgenis to molgenis;
    ```
* Install java (we use adopt [OpenJDK 17](https://adoptium.net/))
* Optionally, if you want to use [scripts](use_scripts_jobs.md) then also install python3
* Download molgenis-emx2-version-all.jar from [releases](https://github.com/molgenis/molgenis-emx2/releases).
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
