package org.molgenis.emx2.datamodels.test;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.TableMetadata.table;

import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.SchemaMetadata;

public class RefAndRefArrayTestExample {
  private RefAndRefArrayTestExample() {
    // hide constructor
  }

  public static void createRefAndRefArrayTestExample(SchemaMetadata schema) {

    ColumnType[] columnTypes =
        new ColumnType[] {UUID, STRING, BOOL, INT, DECIMAL, TEXT, DATE, DATETIME};

    for (ColumnType columnType : columnTypes) {

      String aTableName = columnType.toString() + "_A";
      String fieldName = "AKeyOf" + columnType;
      schema.create(table(aTableName).add(column(fieldName).setType(columnType).setPkey()));

      String bTableName = columnType.toString() + "_B";
      String refFromBToA = "RefToAKeyOf" + columnType;
      String refArrayFromBToA = "RefArrayToAKeyOf" + columnType;

      schema.create(
          table(bTableName)
              .add(column(refFromBToA).setType(REF).setRefTable(aTableName))
              .add(column(refArrayFromBToA).setType(REF_ARRAY).setRefTable(aTableName)));
    }
  }
}
