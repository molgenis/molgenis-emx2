package org.molgenis.emx2.examples.synthetic;

import org.molgenis.*;
import org.molgenis.metadata.SchemaMetadata;
import org.molgenis.metadata.TableMetadata;
import org.molgenis.metadata.Type;

import static org.molgenis.metadata.Type.*;
import static org.molgenis.metadata.Type.DATETIME;

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
