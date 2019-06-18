package org.molgenis.sql;

import org.jooq.Field;
import org.jooq.Record;

import java.util.LinkedHashMap;
import java.util.Map;

public class RowImpl extends org.molgenis.beans.RowBean {

  public RowImpl(Record record) {
    Map<String, Object> values = new LinkedHashMap<>();
    for (Field f : record.fields()) {
      values.put(f.getName(), record.get(f));
    }
    this.set(values);
  }
}
