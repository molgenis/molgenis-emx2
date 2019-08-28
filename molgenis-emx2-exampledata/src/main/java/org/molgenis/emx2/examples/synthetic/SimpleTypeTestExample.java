package org.molgenis.emx2.examples.synthetic;

import org.molgenis.SchemaMetadata;
import org.molgenis.TableMetadata;
import org.molgenis.Type;
import org.molgenis.utils.MolgenisException;

import static org.molgenis.Type.*;
import static org.molgenis.Type.DATETIME;

public class SimpleTypeTestExample {
  public static final String TYPE_TEST = "TypeTest";

  private SimpleTypeTestExample() {
    // hide constructor
  }

  public static void createSimpleTypeTest(SchemaMetadata schema) throws MolgenisException {

    TableMetadata typeTestTable = schema.createTableIfNotExists(TYPE_TEST);
    Type[] types = new Type[] {UUID, STRING, BOOL, INT, DECIMAL, TEXT, DATE, DATETIME};
    for (Type type : types) {

      typeTestTable.addColumn("Test_" + type.toString().toLowerCase(), type);
      typeTestTable
          .addColumn("Test_" + type.toString().toLowerCase() + "_nillable", type)
          .setNullable(true);
    }
  }
}
