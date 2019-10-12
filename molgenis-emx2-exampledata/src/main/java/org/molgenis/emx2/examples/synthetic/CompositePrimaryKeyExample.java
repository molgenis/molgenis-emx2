package org.molgenis.emx2.examples.synthetic;

import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.utils.MolgenisException;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;

import static org.molgenis.emx2.ColumnType.*;

public class CompositePrimaryKeyExample {

  private CompositePrimaryKeyExample() {
    // hide constructor
  }

  public static void createCompositePrimaryKeyExample(SchemaMetadata schema) {
    ColumnType[] columnTypes =
        new ColumnType[] {UUID, STRING, BOOL, INT, DECIMAL, TEXT, DATE, DATETIME};

    for (ColumnType columnType : columnTypes) {
      TableMetadata aTable =
          schema.createTableIfNotExists(columnType.toString() + "_CompositeKeyTable");
      aTable.addColumn("col1", columnType);
      aTable.addColumn("col2", columnType);
      aTable.addColumn("col3");
      aTable.setPrimaryKey("col1", "col2");
    }
  }
}
