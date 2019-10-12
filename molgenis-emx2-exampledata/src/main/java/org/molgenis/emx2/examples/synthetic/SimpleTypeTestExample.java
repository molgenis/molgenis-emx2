package org.molgenis.emx2.examples.synthetic;

import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.utils.MolgenisException;

import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.ColumnType.DATETIME;

public class SimpleTypeTestExample {
  public static final String TYPE_TEST = "TypeTest";

  private SimpleTypeTestExample() {
    // hide constructor
  }

  public static void createSimpleTypeTest(SchemaMetadata schema) {

    TableMetadata typeTestTable = schema.createTableIfNotExists(TYPE_TEST);
    ColumnType[] columnTypes =
        new ColumnType[] {UUID, STRING, BOOL, INT, DECIMAL, TEXT, DATE, DATETIME};
    for (ColumnType columnType : columnTypes) {

      typeTestTable.addColumn("Test_" + columnType.toString().toLowerCase(), columnType);
      typeTestTable
          .addColumn("Test_" + columnType.toString().toLowerCase() + "_nillable", columnType)
          .setNullable(true);
    }
  }
}
