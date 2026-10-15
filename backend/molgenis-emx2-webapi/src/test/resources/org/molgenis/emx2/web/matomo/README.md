# Matomo test snapshot: database, config and auth token

The integration test `MatomoTrackingIntegrationTest` starts Matomo from a **pre-installed snapshot**, so they don't have
to go through Matomo's setup wizard. The snapshot consists of two files:

| File             | Location                                                             | Purpose                                                         |
|------------------|----------------------------------------------------------------------|-----------------------------------------------------------------|
| `dump.sql`       | `src/test/resources/org/molgenis/emx2/web/matomo/gen/dump.sql`       | Database of an installed Matomo, loaded into MariaDB on startup |
| `config.ini.php` | `src/test/resources/org/molgenis/emx2/web/matomo/gen/config.ini.php` | Matomo config that points to that database                      |

This document describes how to create (or regenerate) both files and the auth token the tests use.

## 1. Start a temporary Matomo

Spin up the containers in the docker compose file in this directory:

```bash
docker compose up -d
```

1. Wait until <http://localhost:8081> shows the Matomo installation page.
2. Open <http://localhost:8081> and go through the wizard
    - **Superuser**: for login and password, use `admin`, `secret` respectively. Email can be made up: test@foo.bar
    - **Website**: website name can be emx2 and url http://localhost:8080. Not an Ecommerce site.
3. After the installation wizer, you can log in using the superuser. After that, skip choosing a tracking method.

---

## 3. Create the auth token

The tests need a `token_auth` to query the Matomo API.

1. Log in and click the gear icon (**Administration**).
2. Go to **Personal -> Security**.
3. Under **Auth tokens**, click **Create new token**.
4. Confirm your password and enter a description, for example `integration-test`.
5. Leave **"Only allow secure requests"** checked. The tests send the token in POST requests, which works with that
   setting.
6. Uncheck the **expiration date** by clicking on the check mark.
7. **Copy the token immediately** (Matomo shows it only once) and click the button.

## 4. Export the database

Enter the MariaDB container and make an sql dump:
```bash
docker exec -it <mariadb-container-id> bash
mariadb-dump -umatomo -psecret matomo > /root/dump.sql
```

Then copy the dump out of the container:

```bash
docker cp <mariadb-container-id>:/root/dump.sql ./
```

The dump is placed into the current directory. Optionally, it's nice to add a little disclaimer to the top of the file
to inform others where it's from and how to reproduce:
```sql
/*
 THIS IS A GENERATED SQL DUMP FROM A MARIADB CONTAINER THAT IS CONNECTED TO A MATOMO CONTAINER.

 INSTRUCTIONS FOR RECREATING THIS CAN BE FOUND IN THE README IN THE PARENT OF THIS DIRECTORY.
 */
```

## 5. Export and adjust the config

Copy the config file out of the container:

```bash
docker cp <matomo-container-id>:/var/www/html/config/config.ini.php ./config.ini.php
```

Open `config.ini.php` and under **`[General]`**, add these lines:

```ini
enable_trusted_host_check = 0
enable_browser_archiving_triggering = 0
```

Without the second line, the first tracking request runs Matomo's scheduled tasks and takes longer than the tracker's
5 second timeout.

## 6. Put the files in place

Copy the files to the resources: `backend/molgenis-emx2-webapi/src/test/resources/org/molgenis/emx2/web/matomo/gen`.

## 7. Clean up

```bash
docker compose down -v
```

`-v` also removes the temporary database volume.