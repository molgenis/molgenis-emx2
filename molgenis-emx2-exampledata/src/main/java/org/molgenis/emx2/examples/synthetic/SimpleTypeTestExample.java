package org.molgenis.emx2.examples.synthetic;

import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.ColumnType.DATETIME;
import static org.molgenis.emx2.TableMetadata.table;

public class SimpleTypeTestExample {
  public static final String TYPE_TEST = "TypeTest";

  private SimpleTypeTestExample() {
    // hide constructor
  }

  public static void createSimpleTypeTest(SchemaMetadata schema) {
    TableMetadata typeTestTable = table(TYPE_TEST);
    ColumnType[] columnTypes =
        new ColumnType[] {UUID, STRING, BOOL, INT, DECIMAL, TEXT, DATE, DATETIME};
    for (ColumnType columnType : columnTypes) {
      typeTestTable.addColumn(
          column("Test_" + columnType.toString().toLowerCase()).type(columnType));
      typeTestTable.addColumn(
          column("Test_" + columnType.toString().toLowerCase() + "_nillable")
              .type(columnType)
              .nullable(true));
    }
    schema.create(typeTestTable);
  }
}
