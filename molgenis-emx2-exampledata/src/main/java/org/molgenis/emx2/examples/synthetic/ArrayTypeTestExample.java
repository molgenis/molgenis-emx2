package org.molgenis.emx2.examples.synthetic;

import org.molgenis.*;
import org.molgenis.metadata.SchemaMetadata;
import org.molgenis.metadata.TableMetadata;
import org.molgenis.metadata.Type;

import static org.molgenis.metadata.Type.*;

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
