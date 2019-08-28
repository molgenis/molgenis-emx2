package org.molgenis.emx2.examples.synthetic;

import org.molgenis.emx2.utils.MolgenisException;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.Type;

import static org.molgenis.emx2.Type.*;

public class CompositePrimaryKeyExample {

  private CompositePrimaryKeyExample() {
    // hide constructor
  }

  public static void createCompositePrimaryKeyExample(SchemaMetadata schema)
      throws MolgenisException {
    Type[] types = new Type[] {UUID, STRING, BOOL, INT, DECIMAL, TEXT, DATE, DATETIME};

    for (Type type : types) {
      TableMetadata aTable = schema.createTableIfNotExists(type.toString() + "_CompositeKeyTable");
      aTable.addColumn("col1", type);
      aTable.addColumn("col2", type);
      aTable.addColumn("col3");
      aTable.setPrimaryKey("col1", "col2");
    }
  }
}
