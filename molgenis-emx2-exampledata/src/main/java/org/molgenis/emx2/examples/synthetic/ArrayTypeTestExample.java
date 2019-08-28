package org.molgenis.emx2.examples.synthetic;

import org.molgenis.SchemaMetadata;
import org.molgenis.TableMetadata;
import org.molgenis.Type;
import org.molgenis.utils.MolgenisException;

import static org.molgenis.Type.*;

public class ArrayTypeTestExample {

  private ArrayTypeTestExample() {
    // hide constructor
  }

  public static void createSimpleTypeTest(SchemaMetadata schema) throws MolgenisException {

    TableMetadata typeTestTable = schema.createTableIfNotExists("ArrayTypeTest");
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

      typeTestTable.addColumn("Test_" + type.toString().toLowerCase(), type);
      typeTestTable
          .addColumn("Test_" + type.toString().toLowerCase() + "_nillable", type)
          .setNullable(true);
    }
  }
}
