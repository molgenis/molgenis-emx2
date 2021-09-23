package org.molgenis.emx2.sql;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaInfo;

public class TestSchemaCreateDestroy {
  private static Database db;

  @BeforeClass
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void schemaCrudTest() {
    try {
      db.dropCreateSchema("");
      fail("Schema createTableIfNotExists should fail on empty name");
    } catch (MolgenisException e) {
      System.out.println("Error correctly:\n" + e);
    }

    Schema schema = db.dropCreateSchema(getClass().getSimpleName());

    try {
      db.createSchema(getClass().getSimpleName());
      fail("Schema createTableIfNotExists should fail on duplicated name");
    } catch (MolgenisException e) {
      System.out.println("Error correctly:\n" + e);
    }

    schema.create(table("test"));

    try {
      schema.dropTable("test2");
      fail("Drop schema should fail for unknown table");
    } catch (Exception e) {
      System.out.println("Error correctly:\n" + e);
    }

    schema.dropTable("test");
    assertNull(schema.getTable("test"));

    try {
      db.dropSchema(getClass().getSimpleName() + "fake");
      fail("Drop schema should fail for unknown schema");
    } catch (Exception e) {
      System.out.println("Error correctly:\n" + e);
    }

    db.dropSchema(getClass().getSimpleName());
    assertNull(db.getSchema(getClass().getSimpleName()));
  }

  @Test
  public void testCreateWithDescription() {
    String name = "test_with_desc";
    String desc = "describe me";
    try {
      db.createSchema(name, "describe me");
      assertEquals(name, db.getSchema("test_with_desc").getName());
      assertTrue(db.getSchemaInfos().contains(new SchemaInfo(name, desc)));
    } catch (Exception e) {
      fail("Failed to create schema with description:\n" + e);
    } finally {
      db.dropSchema(name);
    }
  }
}
