package org.molgenis.emx2.examples.synthetic;

import org.molgenis.utils.MolgenisException;
import org.molgenis.SchemaMetadata;
import org.molgenis.TableMetadata;
import org.molgenis.Type;

import static org.molgenis.Type.*;

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
