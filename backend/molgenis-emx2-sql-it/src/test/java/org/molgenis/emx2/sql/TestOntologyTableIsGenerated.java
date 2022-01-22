package org.molgenis.emx2.sql;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;

public class TestOntologyTableIsGenerated {

  private static Database db;

  @BeforeClass
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testOntologyTableIsGenerated() {
    Schema s = db.dropCreateSchema(TestOntologyTableIsGenerated.class.getSimpleName());

    Table table =
        s.create(
            table(
                "test",
                column("name").setPkey(),
                column("code").setType(ColumnType.ONTOLOGY).setRefTable("CodeTable")));
    Table codeTable = s.getTable("CodeTable");
  }
}
