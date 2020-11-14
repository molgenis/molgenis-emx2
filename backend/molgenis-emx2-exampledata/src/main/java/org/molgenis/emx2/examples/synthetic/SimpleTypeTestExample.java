package org.molgenis.emx2.examples.synthetic;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.TableMetadata.table;

import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;

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
      typeTestTable.add(column("Test_" + columnType.toString().toLowerCase()).setType(columnType));
      typeTestTable.add(
          column("Test_" + columnType.toString().toLowerCase() + "_nillable")
              .setType(columnType)
              .setNullable(true));
    }
    schema.create(typeTestTable);
  }
}
