package org.molgenis.emx2.examples.synthetic;

import org.molgenis.MolgenisException;
import org.molgenis.metadata.SchemaMetadata;
import org.molgenis.metadata.TableMetadata;
import org.molgenis.metadata.Type;

import static org.molgenis.metadata.Type.*;

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
