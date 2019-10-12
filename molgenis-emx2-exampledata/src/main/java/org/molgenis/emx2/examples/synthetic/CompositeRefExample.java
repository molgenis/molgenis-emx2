package org.molgenis.emx2.examples.synthetic;

import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.utils.MolgenisException;

import static org.molgenis.emx2.ColumnType.*;

public class CompositeRefExample {

  private CompositeRefExample() {
    // hide constructor
  }

  public static void createCompositeRefExample(SchemaMetadata schema) {
    ColumnType[] columnTypes =
        new ColumnType[] {UUID, STRING, BOOL, INT, DECIMAL, TEXT, DATE, DATETIME};

    for (ColumnType columnType : columnTypes) {
      String aTableName = columnType.toString() + "_A";
      TableMetadata aTable = schema.createTableIfNotExists(aTableName);
      String uniqueColumn1 = "AUnique" + columnType;
      String uniqueColumn2 = "AUnique" + columnType + "2";

      aTable.addColumn(uniqueColumn1, columnType);
      aTable.addColumn(uniqueColumn2, columnType);

      aTable.addUnique(uniqueColumn1, uniqueColumn2);

      String bTableName = columnType.toString() + "_B";
      TableMetadata bTable = schema.createTableIfNotExists(bTableName);
      String refFromBToA1 = "RefToAKeyOf" + columnType;
      String refFromBToA2 = "RefToAKeyOf" + columnType + "2";

      bTable
          .addRefMultiple(refFromBToA1, refFromBToA2)
          .to(aTableName, uniqueColumn1, uniqueColumn2);
    }
  }
}
