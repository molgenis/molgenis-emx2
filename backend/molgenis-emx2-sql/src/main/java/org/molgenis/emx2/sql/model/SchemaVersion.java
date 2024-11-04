package org.molgenis.emx2.sql.model;

import org.jooq.Record;

public record SchemaVersion(String schema, String version, String previousVersion) {
  public SchemaVersion(Record record) {
    this(
        record.get("schema", String.class),
        record.get("version", String.class),
        record.get("previous_version", String.class));
  }
}
