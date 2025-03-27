# Run using docker compose

Steps:

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
