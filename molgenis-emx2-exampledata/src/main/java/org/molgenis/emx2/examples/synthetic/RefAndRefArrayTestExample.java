package org.molgenis.emx2.examples.synthetic;

import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.ColumnType;

import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.ColumnType.DATETIME;

public class RefAndRefArrayTestExample {
  private RefAndRefArrayTestExample() {
    // hide constructor
  }

  public static void createRefAndRefArrayTestExample(SchemaMetadata schema) {

    ColumnType[] columnTypes =
        new ColumnType[] {UUID, STRING, BOOL, INT, DECIMAL, TEXT, DATE, DATETIME};

    for (ColumnType columnType : columnTypes) {

      String aTableName = columnType.toString() + "_A";
      TableMetadata aTable = schema.createTableIfNotExists(aTableName);
      String fieldName = "AKeyOf" + columnType;
      aTable.addColumn(fieldName, columnType);
      aTable.addUnique(fieldName);

      String bTableName = columnType.toString() + "_B";
      TableMetadata bTable = schema.createTableIfNotExists(bTableName);
      String refFromBToA = "RefToAKeyOf" + columnType;
      String refArrayFromBToA = "RefArrayToAKeyOf" + columnType;
      bTable.addRef(refFromBToA, aTableName, fieldName);
      bTable.addRefArray(refArrayFromBToA, aTableName, fieldName);
    }
  }
}
