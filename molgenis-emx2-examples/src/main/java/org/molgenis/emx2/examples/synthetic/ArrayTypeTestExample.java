package org.molgenis.emx2.examples.synthetic;

import org.molgenis.*;

import static org.molgenis.Type.*;

public class ArrayTypeTestExample {

  public static void createSimpleTypeTest(Schema schema) throws MolgenisException {

    Table typeTestTable = schema.createTableIfNotExists("ArrayTypeTest");
    Type[] types =
        new Type[] {
          UUID_ARRAY,
          STRING_ARRAY,
          BOOL_ARRAY,
          INT_ARRAY,
          DECIMAL_ARRAY,
          TEXT_ARRAY,
          DATE_ARRAY,
          DATETIME_ARRAY
        };
    for (Type type : types) {

      Column c = typeTestTable.addColumn("Test_" + type.toString().toLowerCase(), type);
      Column c2 =
          typeTestTable
              .addColumn("Test_" + type.toString().toLowerCase() + "_nillable", type)
              .nullable(true);
    }
  }
}
