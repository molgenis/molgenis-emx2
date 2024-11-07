package org.molgenis.emx2;

import static org.molgenis.emx2.Constants.SYSTEM_SCHEMA;

import org.molgenis.emx2.sql.SchemaVersion;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.sql.model.AppVersion;
import org.molgenis.emx2.tasks.*;

public class SchemaMigrations {

  private static final String DEPENDENCIES =
      """
    certifi==2024.7.4
    charset-normalizer==2.1.1
    idna==3.7
    numpy==1.26.3
    pandas==2.2.2
    python-dateutil==2.8.2
    python-decouple==3.8
    pytz==2022.7.1
    requests==2.32.3
    setuptools==70.0.0
    six==1.16.0
    molgenis-emx2-pyclient==11.23.0
    urllib3==1.26.19
 """;

  public static synchronized void migrate(SqlDatabase db) {
    db.tx(
        tdb ->
            SchemaVersion.getSchemaMigrations(db)
                .forEach(
                    (schema, migrations) -> {
                      for (AppVersion migration : migrations) {
                        System.out.println("Migrating " + migration);
                        // read-in migration script for migration m
                        // apply migration, update schema_metadata version
                        // if a migration is a python script, how do we pass the transaction context
                        // ?, should
                        // we ?
                        // what / who should be the migration user ?
                        Task migrationTask =
                            new ScriptTask(
                                    "run migration for: "
                                        + migration.app()
                                        + " ("
                                        + migration.version()
                                        + ")")
                                .dependencies(DEPENDENCIES)
                                .parameters(migration.app() + ", " + migration.version())
                                .script(
                                    """
                                                     import time
                                                     import numpy as np
                                                     import sys
                                                     # you can get parameters via sys.argv[1]
                                                     print('Do migration: , '+sys.argv[1]+'!', '+sys.argv[1]+'!')
                                                """);

                        TaskServiceInDatabase taskServiceInDatabase =
                            new TaskServiceInDatabase(SYSTEM_SCHEMA);
                        taskServiceInDatabase.submit(migrationTask);

                        while (migrationTask.getStatus() == TaskStatus.RUNNING
                            || migrationTask.getStatus() == TaskStatus.WAITING) {
                          try {
                            Thread.sleep(1000);
                            System.out.println("Migration task is running");
                          } catch (InterruptedException e) {
                            e.printStackTrace();
                          }
                        }
                        System.out.println(
                            "Migration task is done, status is " + migrationTask.getStatus());
                      }
                    }));
  }
}
