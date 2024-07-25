# Run using docker compose

## Requirements

* Install [Docker compose](https://docs.docker.com/compose/install/).

## Download docker compose file

Download the docker compose file to run the basic emx2 installation:

[docker-compose.yml](https://raw.githubusercontent.com/molgenis/molgenis-emx2/master/docker-compose.yml)

## Run docker compose

The following examples are executed in a terminal/console.

Navigate to the folder where you stored  the `docker-compose.yml` and open a terminal, for example:

```console
cd <docker_compose_folder>
```

Make sure docker compose is installed, you can check the installed version for example:

```console
docker compose version
```

Finally run docker compose

```console
docker compose up
```

Press `ctrl` + `c` to stop docker compose

Or to run docker compose in detached mode use:

```console
docker compose up -d
```

To stop docker compose use:

```console
docker compose stop
```

### EMX2 default installation

Make sure `docker compose` is up and running, open a web browser and navigate to:

`localhost:8080`

If everything is setup correctly you should be redirected to:

http://localhost:8080/apps/central/#/

The database `pet store` is installed by default.

You can login as administrator by clicking `Sign in` (top right) and use `admin` as *Username* and *Password*.

### EMX2 Catalogue installation

In order to test and see what emx2 can do we will install a demo catalogue:

* Log in as admin, described previously.
* Navigate back to: http://localhost:8080/apps/central/#/
* Create a new database by clicking on +
  * name: `catalogue`
  * template: DATA_CATALOGUE
  * load example data: true
* Press Create database.
* Navigate in new tab to localhost:8080

If you see the database `catalogue` you have installed the demo catalogue successfully!

### EMX2 Catalogue example with improved UI

* Install [Git](https://github.com/git-guides/install-git).

#### Clone emx2 repository

This will download the latest repository of emx2.

`git clone https://github.com/molgenis/molgenis-emx2.git`

#### Setup environment file

Navigate to the cloned repository `molgenis-emx2` and locate and open the file `apps/nuxt3-ssr/env-sample`. Leave the settings as they are and save the file as:

`molgenis-emx2/apps/nuxt3-ssr/.env`

The dot indicates it is a hidden file so depending on your system OS you might not *see* the .env-sample and .env

#### Run SSR Catalogue

Make sure you have installed the `catalogue` described in the previous [steps](run_docker.md#emx2-catalogue-installation).

* Navigate to catalogue: http://localhost/catalogue/catalogue/#/
* In the top menu bar click `SSR Catalogue`
* You are redirected to: http://localhost/catalogue/ssr-catalogue/

Note:

* If you previously used docker compose make sure you delete the folder `apps/nuxt3-ssr/psql_data` if you want a clean/empty database to work on. Linux users might need to remove this folder with root/sudo permissions.
* You might need to purge some docker settings by running `docker system prune -a`

# Useful commands

To update to latest release, run:

```console
docker-compose pull
```

```console
docker compose up
```

Press `ctrl` + `c` to stop docker compose

Or to run docker compose in detached mode use:

```console
docker compose up -d
```

To stop docker compose use:

```console
docker compose stop
```

To remove unused or conflicting volumes and networks:

```console
docker system prune -a
```

# Notes

* the data of postgresql will be stored in 'psql_data' folder. Remove this folder you want a clean start.
* if you want particular [molgenis-emx2 version](https://hub.docker.com/r/molgenis/molgenis-emx2/tags) then add version in docker-compose.yml file 'molgenis/molgenis-emx2:version'
* `.env-sample` and/or `.env` are hidden file(s), depending on your system OS you might not *see* these by default.