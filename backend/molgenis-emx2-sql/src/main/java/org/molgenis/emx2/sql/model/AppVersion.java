package org.molgenis.emx2.sql.model;

import org.jooq.Record;

public record AppVersion(String app, String version, String previousVersion) {
  public AppVersion(Record record) {
    this(
        record.get("app", String.class),
        record.get("version", String.class),
        record.get("previous_version", String.class));
  }
}
