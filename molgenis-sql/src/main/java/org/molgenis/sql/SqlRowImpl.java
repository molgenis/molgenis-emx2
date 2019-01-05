package org.molgenis.sql;

import org.jooq.Field;
import org.jooq.Record;

import java.util.UUID;

class SqlRowImpl extends SqlRow {

  public SqlRowImpl() {
    super();
  }

  public SqlRowImpl(Record record) {
    super();
    for (Field f : record.fields()) {
      values.put(f.getName(), record.get(f));
    }
  }

  public SqlRowImpl(UUID molgenisid) {
    super(molgenisid);
  }
}
