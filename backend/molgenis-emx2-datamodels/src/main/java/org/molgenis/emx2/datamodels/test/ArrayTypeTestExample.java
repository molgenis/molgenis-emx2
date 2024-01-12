package org.molgenis.emx2.datamodels.test;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.TableMetadata.table;

import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;

public class ArrayTypeTestExample {

  private ArrayTypeTestExample() {
    // hide constructor
  }

  public static void createSimpleTypeTest(SchemaMetadata schema) {

    TableMetadata typeTestTable = schema.create(table("ArrayTypeTest"));
    typeTestTable.add(column("id").setPkey());
    ColumnType[] columnTypes =
        new ColumnType[] {
          UUID_ARRAY,
          STRING_ARRAY,
          BOOL_ARRAY,
          INT_ARRAY,
          DECIMAL_ARRAY,
          TEXT_ARRAY,
          DATE_ARRAY,
          DATETIME_ARRAY
        };
    for (ColumnType columnType : columnTypes) {

      typeTestTable.add(
          column("Test_" + columnType.toString().toLowerCase())
              .setType(columnType)
              .setRequired(true));
      typeTestTable.add(
          column("Test_" + columnType.toString().toLowerCase() + "_nillable").setType(columnType));
    }
  }
}
