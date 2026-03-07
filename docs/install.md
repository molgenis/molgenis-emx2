# How to install and run

MOLGENIS EMX2 can be started in multiple ways:
* using Docker Compose
* using Java and PostgreSQL
* using Kubernetes

## Using Docker Compose

Prerequisites:
* Docker Compose

Steps:
* Download
  molgenis-emx2 <a href="https://raw.githubusercontent.com/mswertz/molgenis-emx2/master/docker-compose.yml" download>
  docker-compose.yml</a> file
* In the directory containing the docker-compose.yml file run: 

  `docker-compose up`

* To update to the latest release, run:

  `docker-compose pull`


Stop the execution of the process by pressing CTRL+C.

Note:
* because PostgreSQL starts slowly EMX2 will restart 2-4 times because of 'ConnectException: Connection refused'. This is
  expected behaviour.
* the data of the PostgreSQL database will be stored in 'psql_data' folder. Remove this folder you want a clean start.
* if you want to use a specific [molgenis-emx2 version](https://hub.docker.com/repository/registry-1.docker.io/mswertz/emx2/tags?page=1)
  add the version number in the docker-compose.yml file with the key `molgenis/molgenis-emx2:version`

## Using Java and your local PostgreSQL database

Prerequisites:
* Java version 21
* PostgreSQL version 15

Steps:
* Download a molgenis-emx2-version-all.jar file from [releases](https://github.com/molgenis/molgenis-emx2/releases).
* Create a PostgreSQL database with name 'molgenis' and with superadmin username/password combination 'molgenis'. 
    ```console
    sudo -u postgres psql
    postgres=# create database molgenis;
    postgres=# create user molgenis with superuser encrypted password 'molgenis';
    postgres=# grant all privileges on database molgenis to molgenis;
    ```
* Start molgenis-emx2
    
  `java -jar molgenis-emx2-<version>-all.jar`

The process will run on HTTP port 8080.

Optionally, the following default values can be modified using either Java properties or using environment variables:
* `MOLGENIS_POSTGRES_URI`
* `MOLGENIS_POSTGRES_USER`
* `MOLGENIS_POSTGRES_PASS`
* `MOLGENIS_HTTP_PORT`
* `MOLGENIS_ADMIN_PW`

For example:

```console
java -DMOLGENIS_POSTGRES_URI=jdbc:postgresql:mydatabase -DMOLGENIS_HTTP_PORT=9090 -jar molgenis-emx2-<version>-all.jar
```

## Using Helm on Kubernetes

If you have Kubernetes server then you can install using [Helm](https://helm.sh/docs/).

Add Helm chart repository (once)

```console
helm repo add emx2 https://github.com/molgenis/molgenis-ops-helm/tree/master/charts/molgenis-emx2
```

Run the latest release (see [Helm docs](https://helm.sh/docs/intro/using_helm/))

```console
helm install emx2/emx2
```

Refresh the Helm repository to get the latest release

```console
helm repo update
```

Alternatively, [download the latest version of Helm Chart](https://github.com/mswertz/molgenis-emx2/tree/master/docs/helm-charts)
