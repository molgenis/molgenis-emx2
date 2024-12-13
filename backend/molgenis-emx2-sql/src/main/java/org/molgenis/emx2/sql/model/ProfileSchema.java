package org.molgenis.emx2.sql.model;

import org.jooq.Record;
import org.molgenis.emx2.sql.appmigrations.Profile;

public record ProfileSchema(String schemaName, Profile profile, int appMigrationVersion) {
  public ProfileSchema(Record record) {
    this(
        record.get("table_schema", String.class),
        Profile.valueOf(record.get("app", String.class)),
        record.get("app_migration_version", Integer.class));
  }
}
