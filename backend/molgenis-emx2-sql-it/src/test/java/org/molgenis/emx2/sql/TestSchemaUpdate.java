package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaInfo;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.molgenis.emx2.TableMetadata.table;

public class TestSchemaUpdate {
  private static Database db;

  @BeforeClass
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testUpdateDescription() {
    String name = "test_with_desc";
    String desc = "describe me";
    String descUpdate = "update me";
    try {
      db.createSchema(name, "describe me");
      assertEquals(name, db.getSchema("test_with_desc").getName());
      assertTrue(db.getSchemaInfos().contains(new SchemaInfo(name, desc)));
      db.updateSchema(name, descUpdate);
      assertTrue(db.getSchemaInfos().contains(new SchemaInfo(name, descUpdate)));
      assertFalse(db.getSchemaInfos().contains(new SchemaInfo(name, desc)));
    } catch (Exception e) {
      fail("Failed to create schema with description");
    } finally {
      db.dropSchema(name);
    }
  }
}
