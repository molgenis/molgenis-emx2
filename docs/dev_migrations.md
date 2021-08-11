# Migrations

In case you develop a breaking change, EMX2 has a simple framework to support migrations.

You should edit the file 'src/main/java/org/molgenis/emx2/sql/Migrations.java'

The idea is that you
* add a migration step for your change 
* that you increment the SOFTWARE_DATABASE_VERSION

The design is such that all migration steps will be run in one transaction, i.e. the update succeeds or fails completely.
In case of a breaking change that cannot be solved with a migration you should throw an exception instead of running a migration.