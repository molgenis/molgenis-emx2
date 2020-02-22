package org.molgenis.emx2.examples.synthetic;

import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.TableMetadata.table;

public class ArrayTypeTestExample {

  private ArrayTypeTestExample() {
    // hide constructor
  }

  public static void createSimpleTypeTest(SchemaMetadata schema) {

    TableMetadata typeTestTable = schema.create(table("ArrayTypeTest"));
    typeTestTable.addColumn(column("id").pkey(true));
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

      typeTestTable.addColumn(
          column("Test_" + columnType.toString().toLowerCase()).type(columnType));
      typeTestTable.addColumn(
          column("Test_" + columnType.toString().toLowerCase() + "_nillable")
              .type(columnType)
              .nullable(true));
    }
  }
}
