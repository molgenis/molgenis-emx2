package org.molgenis.emx2.sql;

import org.jooq.Field;
import org.jooq.Record;
import org.molgenis.emx2.Row;

import java.sql.Array;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class SqlRow extends Row {

  public SqlRow(Record record) throws SQLException {
    Map<String, Object> values = new LinkedHashMap<>();
    for (Field f : record.fields()) {
      Object value = record.get(f);
      if (value instanceof Array) {
        values.put(f.getName(), ((Array) value).getArray());
      } else {
        values.put(f.getName(), value);
      }
    }
    this.set(values);
  }
}
