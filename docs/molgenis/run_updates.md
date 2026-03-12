# Updating your MOLGENIS

To update MOLGENIS you just need to upgrade the *.jar (or in the case of kubernetes/docker, the docker file).

## Migrations

if you deploy a newer MOLGENIS version on existing postgresql instance, MOLGENIS will attempt to migrate your postgresql
schemas such that it keeps on working. If it cannot MOLGENIS will throw an exception and you will need to start with
empty postgresql database. Importantly, migrations might not fix your data so please always read release notes on
breaking changes.

## Delete all schemas tool

To delete all the MOLGENIS generated schemas, roles and users in the postgresql and return to clean state, run command
below. Caution: this will delete all MOLGENIS generated roles (MG_ROLE*, MG_USER*) and all schemas except public

Download the appropriate *.jar release and run command:

```java -cp molgenis-emx2-<version>-all.jar org.molgenis.emx2.sql.AToolToCleanDatabase```

If you have the sourcecode you can also run:
```./gradlew cleandb```

Currently there is no know way to execute this via docker image.