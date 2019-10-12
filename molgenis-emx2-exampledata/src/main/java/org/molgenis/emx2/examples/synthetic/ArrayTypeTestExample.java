package org.molgenis.emx2.examples.synthetic;

import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.utils.MolgenisException;

import static org.molgenis.emx2.ColumnType.*;

public class ArrayTypeTestExample {

  private ArrayTypeTestExample() {
    // hide constructor
  }

  public static void createSimpleTypeTest(SchemaMetadata schema) {

    TableMetadata typeTestTable = schema.createTableIfNotExists("ArrayTypeTest");
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

      typeTestTable.addColumn("Test_" + columnType.toString().toLowerCase(), columnType);
      typeTestTable
          .addColumn("Test_" + columnType.toString().toLowerCase() + "_nillable", columnType)
          .setNullable(true);
    }
  }
}
