package org.molgenis.emx2.sql.model;

import org.jooq.Record;
import org.molgenis.emx2.Profile;

public record ProfileSchema(String schemaName, Profile profile, int appMigrationVersion) {
  public ProfileSchema(Record record) {
    this(
        record.get("table_schema", String.class),
        Profile.valueOf(record.get("profile", String.class)),
        record.get("profile_migration_step", Integer.class));
  }
}
