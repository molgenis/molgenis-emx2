package org.molgenis.sql.psql;

import org.jooq.Field;
import org.jooq.Record;
import org.molgenis.sql.SqlRow;

import java.util.UUID;

public class PsqlRow extends SqlRow {

  public PsqlRow() {
    this(UUID.randomUUID());
  }

  public PsqlRow(Record record) {
    for (Field f : record.fields()) {
      values.put(f.getName(), record.get(f));
    }
  }

  public PsqlRow(UUID molgenisid) {
    if (molgenisid != null) {
      this.values.put(MOLGENISID, molgenisid);
    }
  }
}
