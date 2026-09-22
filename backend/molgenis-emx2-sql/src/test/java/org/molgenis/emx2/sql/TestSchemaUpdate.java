package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;

public class TestSchemaUpdate {
  private static Database db;
  private static final String desc = "describe me";

  @BeforeAll
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
    db.dropCreateSchema(TestSchemaUpdate.class.getSimpleName(), desc);
  }

  @Test
  public void testUpdateDescription() {
    String descUpdate = "update me";
    db.updateSchema(TestSchemaUpdate.class.getSimpleName(), descUpdate);
    assertTrue(
        db.getSchemaInfo(TestSchemaUpdate.class.getSimpleName())
            .description()
            .contains(descUpdate));
    assertFalse(
        db.getSchemaInfo(TestSchemaUpdate.class.getSimpleName()).description().contains(desc));
  }
}
