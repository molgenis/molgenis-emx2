# How to install and run

You can start molgenis-emx2:

* using docker compose
* using java commandline + postgresql
* using kubernetes

Details below:

## Using docker compose

* Install [Docker compose](https://docs.docker.com/compose/install/).
* Download
  molgenis-emx2 <a href="https://raw.githubusercontent.com/mswertz/molgenis-emx2/master/docker-compose.yml" download>
  docker-compose.yml</a> file
* In directory with docker-compose.yml run:

```
docker-compose up
``` 

To update to latest release, run:

```console
docker-compose pull
```

Stop by typing ctrl+c.

N.B.

* because postgres starts slow, emx2 will restart 2-4 times because of 'ConnectException: Connection refused'. This is
  normal.
* the data of postgresql will be stored in 'psql_data' folder. Remove this folder you want a clean start.
* if you want
  particular [molgenis-emx2 version](https://hub.docker.com/repository/registry-1.docker.io/mswertz/emx2/tags?page=1)
  then add version in docker-compose.yml file 'molgenis/molgenis-emx2:version'

## Using java and your own postgresql

* Install java (we use java 20)
* Download a molgenis-emx2-version-all.jar from [releases](https://github.com/molgenis/molgenis-emx2/releases).
* Download and install [Postgresql](https://www.postgresql.org/download/) (we use 14)
* Create postgresql database with name 'molgenis' and with superadmin user/pass 'molgenis'. On Linux/Mac commandline:
    ```console
    sudo -u postgres psql
    postgres=# create database molgenis;
    postgres=# create user molgenis with superuser encrypted password 'molgenis';
    postgres=# grant all privileges on database molgenis to molgenis;
    ```
* Start molgenis-emx2; will run on 8080
    ```console
    java -jar molgenis-emx2-<version>-all.jar
    ```

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

## Using Helm on Kubernetes

If you have Kubernetes server then you can install using [Helm](https://helm.sh/docs/).

Add helm chart repository (once)

```console
helm repo add emx2 https://github.com/molgenis/molgenis-ops-helm/tree/master/charts/molgenis-emx2
```

Run the latest release (see [Helm docs](https://helm.sh/docs/intro/using_helm/))

```console
helm install emx2/emx2
```

Update helm repository to get newest release

```console
helm repo update
```

Alternatively, [download latest helm chart](https://github.com/mswertz/molgenis-emx2/tree/master/docs/helm-charts)
