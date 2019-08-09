package org.molgenis.emx2.examples.synthetic;

import org.molgenis.MolgenisException;
import org.molgenis.Schema;
import org.molgenis.Table;
import org.molgenis.Type;

import static org.molgenis.Type.*;

public class RefTestExample {

  public static void createRefTestExmple(Schema schema) throws MolgenisException {

    Type[] types = new Type[] {UUID, STRING, BOOL, INT, DECIMAL, TEXT, DATE, DATETIME};

    for (Type type : types) {

      String A = type.toString() + "_A";
      Table aTable = schema.createTableIfNotExists(A);
      String fieldName = "AKeyOf" + type;
      aTable.addColumn(fieldName, type);
      aTable.addUnique(fieldName);

      String B = type.toString() + "_B";
      Table bTable = schema.createTableIfNotExists(B);
      String refFromBToA = "RefToAKeyOf" + type;
      bTable.addRef(refFromBToA, A, fieldName);
    }
  }
}
