package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.utils.MolgenisException;

import java.sql.SQLException;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertNull;

public class TestSchemaCreateDestroy {
  private static Database db;

  @BeforeClass
  public static void setUp() throws SQLException {
    db = DatabaseFactory.getTestDatabase();
  }

  @Test
  public void schemaCrudTest() {
    try {
      db.createSchema("");
      fail("Schema createTableIfNotExists should fail on empty name");
    } catch (MolgenisException e) {
      System.out.println("Error correctly:\n" + e);
    }

    Schema schema = db.createSchema(getClass().getSimpleName());

    try {
      db.createSchema(getClass().getSimpleName());
      fail("Schema createTableIfNotExists should fail on duplicated name");
    } catch (MolgenisException e) {
      System.out.println("Error correctly:\n" + e);
    }

    schema.createTableIfNotExists("test");

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
    try {
      db.getSchema(getClass().getSimpleName());
      fail("Schema should have been dropped");
    } catch (Exception e) {
      System.out.println("Error correctly:\n" + e);
    }
  }
}
