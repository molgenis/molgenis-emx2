# Run using docker compose

## Linux / Docker Desktop

These instructions can be followed when using Linux or any OS if using Docker Desktop.

* Install [Docker compose](https://docs.docker.com/compose/install/).
* Download
  molgenis-emx2 <a href="https://github.com/molgenis/molgenis-emx2/blob/master/docker-compose.yml" download>
  docker-compose.yml</a> file
* In directory with docker-compose.yml run:

  ```
  docker-compose up
  ```

* Open on http://localhost:8080

Useful commands:

To update to latest release, run:

```console
docker-compose pull
```

Stop by typing ctrl+c.

## macOS (without Docker Desktop)

These instructions were written with Apple Silicon in mind.
While [Colima](https://colima.run/) is used to run Docker Engine, other approaches should be viable as well.

1. Install [Colima](https://colima.run/) as defined in [these instructions](https://colima.run/docs/installation/#macos).
2. Install Docker CLI [as described here](https://docs.docker.com/engine/install/binaries/#install-client-binaries-on-macos). In short:
    ```bash
    curl -OL https://download.docker.com/mac/static/stable/aarch64/docker-<version>.tgz
    tar xzvf docker-<version>.tgz
    sudo xattr -rc docker
    sudo mv docker/docker /usr/local/bin/
    ```
3. Validate if Docker is installed correctly by running `docker` (should return usage)
4. Run `mkdir -p ~/.docker/cli-plugins`
5. Install Docker compose (using [https://github.com/docker/compose/releases/](https://github.com/docker/compose/releases/)):
    ```bash
    curl -OL https://github.com/docker/compose/releases/latest/download/docker-compose-darwin-aarch64
    sudo xattr -rc docker-compose-darwin-aarch64
    chmod u+x docker-compose-darwin-aarch64
    mv docker-compose-darwin-aarch64 ~/.docker/cli-plugins/docker-compose
    ```
6. Run `colima start`
7. Validate docker through `docker info` (there should be server information available)
8. Download the [docker-compose.yml](https://github.com/molgenis/molgenis-emx2/blob/master/docker-compose.yml)
9. Run `docker compose up` from the directory where the `docker-compose.yml` is stored.
10. Open http://localhost:8080/

!> After booting your computer, be sure to run `colima start` again before running docker commands.

## Postgres

When running docker-compose up, a folder is created in the directory where the docker-compose.yml is located. This folder is called ```psql_data``` 


You can delete docker images and/or containers without losing the database data. So you can just upgrade anytime, the data will be intact.

> If you really need to keep the data, always make sure that you have a backup.

If you want to have a clean database, for example when you are testing things locally, you can remove this folder manually in the file explorer or finder. 

You can also run:

```
rm -Rf psql_data
```

**NB:** This action is not recoverable.

## Notes:

* because postgres starts slow, emx2 will restart 2-4 times because of 'ConnectException: Connection refused'. This is
  normal.
* the data of postgresql will be stored in 'psql_data' folder. Remove this folder you want a clean start.
* if you want
  particular [molgenis-emx2 version](https://hub.docker.com/r/molgenis/molgenis-emx2/tags)
  then add version in docker-compose.yml file 'molgenis/molgenis-emx2:version'
