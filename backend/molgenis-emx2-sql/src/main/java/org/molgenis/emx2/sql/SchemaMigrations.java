package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;
import static org.molgenis.emx2.sql.MetadataUtils.MOLGENIS;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Objects;
import org.jooq.DSLContext;
import org.molgenis.emx2.sql.model.SchemaMetadata;
import org.molgenis.emx2.sql.model.SchemaVersion;

public class SchemaMigrations {
  public static synchronized void migrate(SqlDatabase db) {
    db.tx(
        tdb -> {
          DSLContext ctx = ((SqlDatabase) tdb).getJooq();

          // fetch all schema metadata ( maybe only the data we need ?)
          List<SchemaMetadata> schemaMetadatas =
              ctx.selectFrom(table(name(MOLGENIS, "schema_metadata"))).fetch().stream()
                  .map(SchemaMetadata::new)
                  .toList();

          // fetch all version data ( could filter by schema or group here, but in memory is fine I
          // guess)
          List<SchemaVersion> schemaVersions =
              ctx.selectFrom(table(name(MOLGENIS, "schema_version"))).fetch().stream()
                  .map(SchemaVersion::new)
                  .toList();

          // now loop over the schemas and apply the migrations needed
          for (SchemaMetadata schemaMetadata : schemaMetadatas) {
            String currentVersion = schemaMetadata.version();
            if (currentVersion == null) {
              // no versioning for this schema ( could also filter in query)
              continue;
            }
            List<SchemaVersion> versions =
                new java.util.ArrayList<>(
                    schemaVersions.stream()
                        .filter(v -> v.schema().equals(schemaMetadata.tableName()))
                        .toList());

            if (versions.isEmpty()) {
              // no migrations for this schema
              continue;
            }

            // the version history is stored as a linked list, sort in into an ordered list
            versions.sort((a, b) -> Objects.equals(a.version(), b.previousVersion()) ? 0 : -1);

            // walk backwards through the versions to que the migrations that have not been
            // applied
            List<SchemaVersion> migrations = Lists.newArrayList();
            for (SchemaVersion version : versions) {
              if (version.version().equals(currentVersion)) {
                // already at the correct version
                break;
              }
              migrations.add(version);
            }

            // now we have a single list of migrations that need to be run in order
            for (SchemaVersion migration : migrations) {
              System.out.println("Migrating(dry run " + migration);
              // read-in migration script for migration m
              // apply migration, update schema_metadata version
              // if a migration is a python script, how do we pass the transaction context ?, should
              // we ?
              // what / who should be the migration user ?
            }
          }
        });
  }
}
