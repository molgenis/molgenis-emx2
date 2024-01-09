package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.SelectColumn.s;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Query;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.datamodels.test.CrossSchemaReferenceExample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestCrossSchemaForeignKeysAndInheritance {
  private static Logger logger =
      LoggerFactory.getLogger(TestCrossSchemaForeignKeysAndInheritance.class);

  private static final String schemaName1 =
      TestCrossSchemaForeignKeysAndInheritance.class.getSimpleName() + "1";
  private static final String schemaName2 =
      TestCrossSchemaForeignKeysAndInheritance.class.getSimpleName() + "2";

  static Schema schema1;
  static Schema schema2;
  static Database db;

  @BeforeAll
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
    db.dropSchemaIfExists(schemaName2);
    db.dropSchemaIfExists(schemaName1);
    schema1 = db.createSchema(schemaName1);
    schema2 = db.createSchema(schemaName2);
    CrossSchemaReferenceExample.create(schema1, schema2);
  }

  @Test
  public void testRef() {
    Query q = schema2.getTable("Child").select(s("name"), s("parent", s("name"), s("hobby")));
    assertTrue(q.retrieveJSON().contains("stamps"));
    assertEquals("stamps", q.retrieveRows().get(0).getString("parent-hobby"));
  }

  @Test
  public void testRefArray() {
    Query q = schema2.getTable("PetLover").select(s("name"), s("pets", s("name"), s("species")));
    assertEquals("dog", q.retrieveRows().get(1).getString("pets-species"));

    System.out.println(q.retrieveJSON());
    assertTrue(q.retrieveJSON().contains("dog"));
  }

  @Test
  public void testInheritance() {
    Query q = schema2.getTable("Mouse").select(s("name"), s("species"));
    assertEquals("mickey", q.retrieveRows().get(0).getString("name"));

    q = schema1.getTable("Pet").select(s("name"), s("species"));
    assertEquals(3, q.retrieveRows().size());
  }

  @Test
  public void testForeignKeyBlocksDeleteAndDrop() {
    Table schema1pet = schema1.getTable("Pet");
    try {
      schema1pet.delete(row("name", "pooky"));
      fail("should not be able to delete ref_array cross schema");
    } catch (Exception e) {
      logger.debug("Errored correctly: " + e.getMessage());
    }

    try {
      schema1pet.getMetadata().drop();
      fail("should not be able to drop ref_array cross schema");
    } catch (Exception e) {
      logger.debug("Errored correctly: " + e.getMessage());
    }

    Table schema1parent = schema1.getTable("Parent");
    try {
      schema1parent.delete(row("name", "parent1"));
      fail("should not be able to delete ref cross schema");
    } catch (Exception e) {
      logger.debug("Errored correctly: " + e.getMessage());
    }

    try {
      schema1parent.getMetadata().drop();
      fail("should not be able to delete ref cross schema");
    } catch (Exception e) {
      logger.debug("Errored correctly: " + e.getMessage());
    }

    try {
      db.dropSchema(schema1.getName());
      fail("should not be able to delete schema if other schemas depend on it");
    } catch (Exception e) {
      logger.debug("Errored correctly: " + e.getMessage());
    }
  }
}
