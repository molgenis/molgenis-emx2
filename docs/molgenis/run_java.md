# Run using java and postgresql

Steps:

* Install [Postgresql 15](https://www.postgresql.org/download/)
* Create postgresql database with name 'molgenis' and with superadmin user/pass 'molgenis'. On Linux/Mac commandline:

    ```console
    psql postgres
    ```

    Or on macOS:

    ```console
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

* Open on <http://localhost:8080>

Optionally, you can change defaults using either java properties or using env variables:

* `MOLGENIS_POSTGRES_URI`
* `MOLGENIS_POSTGRES_USER`
* `MOLGENIS_POSTGRES_PASS`
* `MOLGENIS_HTTP_PORT`
* `MOLGENIS_ADMIN_PW`
* `MOLGENIS_CONTEXT_PATH` — serve the application under a URL prefix (e.g. `/molgenis`), useful when running behind a reverse proxy

For example:

```console
java -DMOLGENIS_POSTGRES_URI=jdbc:postgresql:mydatabase -DMOLGENIS_HTTP_PORT=9090 -jar molgenis-emx2-<version>-all.jar
```

## Subpath deployment

To run MOLGENIS under a URL prefix (e.g. `https://host/molgenis/`) instead of the root, set the context path at startup:

```console
MOLGENIS_CONTEXT_PATH=/molgenis java -jar molgenis-emx2-<version>-all.jar
```

Javalin serves all routes under `/molgenis`. Internally generated redirects (e.g. to `/apps/central/`) are automatically prefixed, and the OIDC callback URL includes the prefix.

When bundling a custom frontend build, also set the matching build-time variables:

```console
VITE_BASE_PATH=/molgenis npm run build      # Vite-based apps
NUXT_APP_BASE_URL=/molgenis/ npm run build  # ui Nuxt app
```

Configure your reverse proxy to forward the prefix **unchanged** (do not strip it):

Traefik:

```yaml
labels:
  - "traefik.http.routers.molgenis.rule=PathPrefix(`/molgenis`)"
  - "traefik.http.services.molgenis.loadbalancer.server.port=8080"
```

Nginx:

```nginx
location /molgenis/ {
    proxy_pass http://javalin:8080/molgenis/;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

Summary of relevant environment variables:

| Variable | Scope | Example | Description |
| -------- | ----- | ------- | ----------- |
| `MOLGENIS_CONTEXT_PATH` | backend runtime | `/molgenis` | URL prefix for Javalin router |
| `VITE_BASE_PATH` | Vite build-time | `/molgenis` | asset base path for Vite apps |
| `NUXT_APP_BASE_URL` | Nuxt build/runtime | `/molgenis/` | base URL for the `ui` Nuxt app |
| `NUXT_PUBLIC_API_BASE` | catalogue SSR runtime | `https://host/molgenis/` | full API URL for the `catalogue` SSR app |

## Metrics

MOLGENIS enables metrics api for example to use with prometheus. By default this is disabled. Please on use configure carefully to avoid abuse.

* `MOLGENIS_METRICS_ENABLED=true` enables the metrics on path /api/metrics
* `MOLGENIS_METRICS_PATH=/api/metric` enables the metrics path to be customized

For example:

```console
java -DMOLGENIS_METRICS_ENABLED=true -jar molgenis-emx2-<version>-all.jar
```

## Tips

### Logging

To enable more detailed logging, you can configure the `log4j2.level` JVM option. For example:

```shell
java -jar app.jar -Dlog4j2.level=DEBUG
```

This option sets the logging level according to the standard [Log4j2 logging levels](https://logging.apache.org/log4j/2.x/manual/customloglevels.html).

### On mac you can install postgres using [homebrew](https://formulae.brew.sh/formula/postgresql)

* to start/stop the service

```shell
brew services start postgresql
brew services stop postgresql
brew services restart postgresql
```

* to completely wipe postgresql:

```console
brew services stop postgresql
rm -R /opt/homebrew/var/postgres
initdb -d  /opt/homebrew/var/postgres
brew services restart postgresql 
```

## FAQ

If you previously had an installation of Molgenis and want to start fresh, check out the [delete all schemas tool document section](/apps/docs/#/molgenis/run_updates?id=delete-all-schemas-tool)

Alternatively, if you want to execute the ```SQL``` on the database yourself, [here is a link](https://github.com/molgenis/molgenis-emx2/tree/master/backend/molgenis-emx2-sql/src/main/resources/org/molgenis/emx2/sql/utility-sql) to the files.
