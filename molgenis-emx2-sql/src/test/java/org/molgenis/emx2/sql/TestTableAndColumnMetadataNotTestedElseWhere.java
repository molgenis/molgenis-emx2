package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.MolgenisException;

import static junit.framework.TestCase.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

public class TestTableAndColumnMetadataNotTestedElseWhere {
  private static Database db;

  @BeforeClass
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testDuplicateColumnError() {
    try {
      SchemaMetadata s = db.createSchema("testDuplicateColumnError").getMetadata();
      TableMetadata t = s.create(table("test").addColumn(column("test")));
      System.out.println(t);

      t.addColumn(column("test"));
      fail("should not be able to add same column twice");
    } catch (MolgenisException me) {
      System.out.println("Error correctly:\n" + me);
    }
  }

  @Test
  public void testRemoveUnknownUniqueError() {
    try {
      SchemaMetadata s = db.createSchema("testRemoveUnknownUniqueError").getMetadata();
      TableMetadata t = s.create(table("test").addUnique("test"));
      fail("should not be able to set unique on not existing column");
    } catch (MolgenisException me) {
      System.out.println("Error correctly:\n" + me);
    }
  }

  @Test
  public void testUniques() {
    SchemaMetadata s = db.createSchema("testUniques").getMetadata();
    TableMetadata t =
        s.create(table("test").addColumn(column("a").pkey(true)).addColumn(column("b")));
    t.addUnique("a", "b");
    assertTrue(t.isUnique("b", "a")); // order doesn't matter
    t.removeUnique("b", "a");
    assertEquals(0, t.getUniques().size());
  }
}
