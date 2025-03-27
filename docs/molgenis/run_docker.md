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

To delete the database run (not recoverable!):
```
rm -Rf psql_data
```

Notes:

* because postgres starts slow, emx2 will restart 2-4 times because of 'ConnectException: Connection refused'. This is
  normal.
* the data of postgresql will be stored in 'psql_data' folder. Remove this folder you want a clean start.
* if you want
  particular [molgenis-emx2 version](https://hub.docker.com/r/molgenis/molgenis-emx2/tags)
  then add version in docker-compose.yml file 'molgenis/molgenis-emx2:version'
