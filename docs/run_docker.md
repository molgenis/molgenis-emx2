# Run using docker compose

Steps:

* Install [Docker compose](https://docs.docker.com/compose/install/).
* Download
  molgenis-emx2 <a href="https://raw.githubusercontent.com/mswertz/molgenis-emx2/master/docker-compose.yml" download>
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

Notes:

* because postgres starts slow, emx2 will restart 2-4 times because of 'ConnectException: Connection refused'. This is
  normal.
* the data of postgresql will be stored in 'psql_data' folder. Remove this folder you want a clean start.
* if you want
  particular [molgenis-emx2 version](https://hub.docker.com/repository/registry-1.docker.io/mswertz/emx2/tags?page=1)
  then add version in docker-compose.yml file 'molgenis/molgenis-emx2:version'