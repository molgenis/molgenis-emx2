package org.molgenis.sql;

import org.jooq.Field;
import org.jooq.Record;

import java.util.LinkedHashMap;
import java.util.Map;

public class SqlRow extends org.molgenis.beans.RowBean {

  public SqlRow(Record record) {
    Map<String, Object> values = new LinkedHashMap<>();
    for (Field f : record.fields()) {
      values.put(f.getName(), record.get(f));
    }
    this.set(values);
  }
}
