package org.molgenis.emx2.sql;

import org.junit.Before;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

public class TestCompositeForeignKeys {
  private Schema schema;

  @Before
  public void setUp() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(TestCompositeForeignKeys.class.getSimpleName());

    // create target table
    schema.create(
        table("TargetTable", column("key1a").pkey(), column("key1b").pkey()),
        table(
            "ReferenceTable",
            column("fkey1").ref("TargetTable"),
            column("fkey2b").ref("TargetTable")));
  }
}
