package org.molgenis.emx2.sql.model;

import org.jooq.Record;
import org.molgenis.emx2.Profile;

public record ProfileSchema(String schemaName, Profile profile, int appMigrationVersion) {
  public ProfileSchema(Record jooqRecord) {
    this(
        jooqRecord.get("table_schema", String.class),
        Profile.valueOf(jooqRecord.get("profile", String.class)),
        jooqRecord.get("profile_migration_step", Integer.class));
  }
}
