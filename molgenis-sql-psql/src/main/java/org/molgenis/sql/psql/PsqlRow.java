package org.molgenis.sql.psql;

import com.fasterxml.uuid.Generators;
import org.jooq.Field;
import org.jooq.Record;
import org.molgenis.sql.SqlRow;

import java.util.UUID;

public class PsqlRow extends SqlRow {

  public PsqlRow() {
    // to ensure we have nicely sorted record we have time based uuid
    this(Generators.timeBasedGenerator().generate());
  }

  public PsqlRow(Record record) {
    for (Field f : record.fields()) {
      values.put(f.getName(), record.get(f));
    }
  }

  public PsqlRow(UUID molgenisid) {
    super(molgenisid);
  }
}
