package org.molgenis.emx2.sql.model;

import org.jooq.Record;
import org.molgenis.emx2.sql.appmigrations.App;

public record AppSchema(String schemaName, App app, int appMigrationVersion) {
  public AppSchema(Record record) {
    this(
        record.get("table_schema", String.class),
        App.valueOf(record.get("app", String.class)),
        record.get("app_migration_version", Integer.class));
  }
}
