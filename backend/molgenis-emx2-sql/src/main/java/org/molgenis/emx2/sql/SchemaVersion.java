package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;
import static org.molgenis.emx2.sql.MetadataUtils.MOLGENIS;

import com.google.common.collect.Lists;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jooq.DSLContext;
import org.molgenis.emx2.sql.model.AppVersion;
import org.molgenis.emx2.sql.model.SchemaMetadata;

public class SchemaVersion {
  private SchemaVersion() {}

  public static Map<String, List<AppVersion>> getSchemaMigrations(SqlDatabase db) {
    Map<String, List<AppVersion>> schemaMigrations = new HashMap<>();
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
            schemaMigrations.put(schemaMetadata.tableName(), migrations);
          }
        });
    return schemaMigrations;
  }
}
;
