package org.molgenis.emx2.sql;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.SchemaInfo;

public class TestSchemaUpdate {
  private static Database db;
  private static final String desc = "describe me";

  @BeforeClass
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
    db.dropCreateSchema(TestSchemaUpdate.class.getName(), desc);
  }

  @Test
  public void testUpdateDescription() {
    String descUpdate = "update me";
    db.updateSchema(TestSchemaUpdate.class.getName(), descUpdate);
    assertTrue(
        db.getSchemaInfos()
            .contains(new SchemaInfo(TestSchemaUpdate.class.getName(), descUpdate, false)));
    assertFalse(
        db.getSchemaInfos()
            .contains(new SchemaInfo(TestSchemaUpdate.class.getName(), desc, false)));
  }
}
