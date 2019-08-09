package org.molgenis.emx2.examples.synthetic;

import org.molgenis.*;

import static org.molgenis.Type.*;
import static org.molgenis.Type.DATETIME;

public class RefAndRefArrayTestExample {
  private RefAndRefArrayTestExample() {
    // hide constructor
  }

  public static void createRefAndRefArrayTestExample(Schema schema) throws MolgenisException {

    Type[] types = new Type[] {UUID, STRING, BOOL, INT, DECIMAL, TEXT, DATE, DATETIME};

    for (Type type : types) {

      String aTableName = type.toString() + "_A";
      Table aTable = schema.createTableIfNotExists(aTableName);
      String fieldName = "AKeyOf" + type;
      aTable.addColumn(fieldName, type);
      aTable.addUnique(fieldName);

      String bTableName = type.toString() + "_B";
      Table bTable = schema.createTableIfNotExists(bTableName);
      String refFromBToA = "RefToAKeyOf" + type;
      String refArrayFromBToA = "RefArrayToAKeyOf" + type;
      bTable.addRef(refFromBToA, aTableName, fieldName);
      bTable.addRefArray(refArrayFromBToA, aTableName, fieldName);
    }
  }
}
