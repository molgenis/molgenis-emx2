package org.molgenis.emx2.datamodels.test;

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
    TableMetadata typeTestTable = table(TYPE_TEST).add(column("id").setPkey());
    ColumnType[] columnTypes =
        new ColumnType[] {UUID, STRING, BOOL, INT, DECIMAL, TEXT, DATE, DATETIME};
    for (ColumnType columnType : columnTypes) {
      typeTestTable.add(
          column("Test " + columnType.toString().toLowerCase())
              .setType(columnType)
              .setRequired(true));
      typeTestTable.add(
          column("Test " + columnType.toString().toLowerCase() + " nillable").setType(columnType));
    }
    schema.create(typeTestTable);
  }
}
