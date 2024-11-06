package org.molgenis.emx2.sql.model;

import org.jooq.Record;

public record SchemaMetadata(
    String tableName, String description, String settings, String app, String version) {
  public SchemaMetadata(Record record) {
    this(
        record.get("table_schema", String.class),
        record.get("description", String.class),
        record.get("settings", String.class),
        record.get("app", String.class),
        record.get("version", String.class));
  }
}
