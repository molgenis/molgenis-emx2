package org.molgenis.emx2.io.emx2b;

import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;

import java.util.List;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

public class Emx3 {

  public static SchemaMetadata readSchema(List<Row> rows) {

    SchemaMetadata schema = new SchemaMetadata();

    for (Row r : rows) {
      String tableName = r.getString("tableName");

      if (schema.getTableMetadata(tableName) == null) {
        schema.create(table(tableName));
      }

      // load table metadata, this is when columnName is empty
      if (!r.containsName("columnName")) {
        schema.getTableMetadata(tableName).setDescription(r.getString("description"));
        schema.getTableMetadata(tableName).setInherit(r.getString("tableExtends"));
      }

      // load column metadata
      else {
        Column column = column(r.getString("columnName"));
        if (r.notNull("columnType"))
          column.type(ColumnType.valueOf(r.getString("columnType").toUpperCase()));
        if (r.notNull("key")) column.key(r.getInteger("key"));
        if (r.notNull("ref")) column.refTable(r.getString("ref"));
        if (r.notNull("mappedBy")) column.mappedBy(r.getString("mappedBy"));
        if (r.notNull("nullable")) column.nullable(r.getBoolean("nullable"));

        schema.getTableMetadata(tableName).add(column);
      }
    }
    return schema;
  }
}
