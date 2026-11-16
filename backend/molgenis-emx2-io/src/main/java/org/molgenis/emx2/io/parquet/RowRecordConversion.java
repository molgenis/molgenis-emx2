package org.molgenis.emx2.io.parquet;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.molgenis.emx2.Row;

public final class RowRecordConversion {

  public static final String COLUMN_NAME_PROPERTY = "emx2.name";

  private RowRecordConversion() {
    // Hide constructor for utility classes
  }

  public static Row toRow(GenericRecord record) {
    Row row = new Row();
    for (Schema.Field field : record.getSchema().getFields()) {
      Object value = record.get(field.pos());
      if (value instanceof CharSequence) {
        value = value.toString();
      }
      row.set(columnName(field), value);
    }
    return row;
  }

  private static String columnName(Schema.Field field) {
    String columnName = field.getProp(COLUMN_NAME_PROPERTY);
    if (columnName == null) {
      return field.name();
    }
    return columnName;
  }
}
