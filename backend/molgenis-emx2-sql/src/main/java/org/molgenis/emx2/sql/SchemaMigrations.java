package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;
import static org.molgenis.emx2.sql.MetadataUtils.MOLGENIS;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Objects;
import org.jooq.DSLContext;
import org.molgenis.emx2.sql.model.AppVersion;
import org.molgenis.emx2.sql.model.SchemaMetadata;

public class SchemaMigrations {
  public static synchronized void migrate(SqlDatabase db) {
    db.tx(
        tdb -> {
          DSLContext ctx = ((SqlDatabase) tdb).getJooq();

          // fetch all schema metadata ( maybe only the data we need ?)
          List<SchemaMetadata> schemas =
              ctx.selectFrom(table(name(MOLGENIS, "schema_metadata"))).fetch().stream()
                  .map(SchemaMetadata::new)
                  .toList();

          // fetch all version data
          List<AppVersion> appVersions =
              ctx.selectFrom(table(name(MOLGENIS, "app_version"))).fetch().stream()
                  .map(AppVersion::new)
                  .toList();

          // now loop over the schemas and apply the migrations needed
          for (SchemaMetadata schemaMetadata : schemas) {
            String currentVersion = schemaMetadata.version();
            String app = schemaMetadata.app();
            if (currentVersion == null || app == null) {
              // no versioning for this schema/app combo
              continue;
            }
            List<AppVersion> versions =
                new java.util.ArrayList<>(
                    appVersions.stream().filter(av -> av.app().equals(app)).toList());

            if (versions.isEmpty()) {
              // no migrations for this schema/app combo
              continue;
            }

            // the version history is stored as a linked list, sort in into an ordered list
            versions.sort((a, b) -> Objects.equals(a.version(), b.previousVersion()) ? 0 : -1);

            // walk backwards through the versions to que the migrations that have not been
            // applied
            List<AppVersion> migrations = Lists.newArrayList();
            for (AppVersion version : versions) {
              if (version.version().equals(currentVersion)) {
                // already at the correct version
                break;
              }
              migrations.add(version);
            }

            // now we have a single list of migrations that need to be run in order
            for (AppVersion migration : migrations) {
              System.out.println("Migrating " + migration);
              // read-in migration script for migration m
              // apply migration, update schema_metadata version
              // if a migration is a python script, how do we pass the transaction context ?, should
              // we ?
              // what / who should be the migration user ?
              Task migrationTask =
                  new ScriptTask(
                          "run migration for: "
                              + migration.app()
                              + " ("
                              + migration.version()
                              + ")")
                      .dependencies(
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
                                            molgenis_emx2_pyclient
                                            urllib3==1.26.19
                                       """)
                      .parameters(migration.app() + ", " + migration.version())
                      .script(
                          """
                                                    import time
                                                    import numpy as np
                                                    import sys
                                                    # you can get parameters via sys.argv[1]
                                                    print('Do migration: , '+sys.argv[1]+'!', '+sys.argv[1]+'!')
                                                    """);
            }
          }
        });
  }
}
