package org.molgenis.emx2.database;

import org.molgenis.sql.SqlRow;

import java.util.Map;
import java.util.UUID;

public class EmxRow extends SqlRow {

  public EmxRow() {
    super();
  }

  public UUID getId() {
    return (UUID) values.get(MOLGENISID);
  }

  public Map<String, Object> toMap() {
    return values;
  }
}
