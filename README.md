[![Build Status](https://travis-ci.com/molgenis/molgenis-emx2.svg?branch=master)](https://travis-ci.com/molgenis/molgenis-emx2)
[![Quality Status](https://sonarcloud.io/api/project_badges/measure?project=molgenis_molgenis-emx2&metric=alert_status)](https://sonarcloud.io/dashboard?id=molgenis_molgenis-emx2)
[![SonarCloud Coverage](https://sonarcloud.io/api/project_badges/measure?project=molgenis_molgenis-emx2&metric=coverage)](https://sonarcloud.io/component_measures/metric/coverage/list?id=molgenis_molgenis-emx2)
[![semantic-release](https://img.shields.io/badge/%20%20%F0%9F%93%A6%F0%9F%9A%80-semantic--release-e10079.svg)](https://github.com/semantic-release/semantic-release)

# molgenis-emx2 BETA

This is a BETA implementation of new MOLGENIS/EMX2 data service. Core differences with molgenis/molgenis include that it
is simpler to setup and operate, it allows multiple data schemas and that for developers it exposes data via a
self-documenting graphql api.

Demo server: https://emx2.test.molgenis.org/

## How to run

You can start molgenis-emx2:

* using docker compose
* using java commandline + postgresql
* using kubernetes

Details below:

### 1. Using docker compose

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

### 2. Using java and your own postgresql

* Install java (we use java 11 or higher)
* Download a molgenis-emx2-version-all.jar from [releases](https://github.com/mswertz/molgenis-emx2/releases).
* Download and install [Postgresql](https://www.postgresql.org/download/) (we use 11 or higher)
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

For example:

```console
java -DMOLGENIS_POSTGRES_URI=jdbc:postgresql:mydatabase -DMOLGENIS_HTTP_PORT=9090 -jar molgenis-emx2-<version>-all.jar
```

### 3. Using Helm on Kubernetes

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

# For developers

Find developer documentation [here](../master/DEVELOP.md)
