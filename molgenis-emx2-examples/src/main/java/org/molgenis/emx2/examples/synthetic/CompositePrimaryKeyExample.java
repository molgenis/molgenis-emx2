package org.molgenis.emx2.examples.synthetic;

import org.molgenis.MolgenisException;
import org.molgenis.Schema;
import org.molgenis.Table;
import org.molgenis.Type;

import static org.molgenis.Type.*;

public class CompositePrimaryKeyExample {

  public static void createCompositePrimaryExample(Schema schema) throws MolgenisException {
    Type[] types = new Type[] {UUID, STRING, BOOL, INT, DECIMAL, TEXT, DATE, DATETIME};

    for (Type type : types) {
      Table aTable = schema.createTableIfNotExists(type.toString() + "_CompositeKeyTable");
      aTable.addColumn("col1", type);
      aTable.addColumn("col2", type);
      aTable.addColumn("col3");
      aTable.setPrimaryKey("col1", "col2");
    }
  }
}
