package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

public class TestOntologyTableIsGenerated {

  private static Database db;

  @BeforeAll
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testOntologyTableIsGenerated() {
    Schema s = null;

    // test error is thrown if refschema doesn't exist
    try {
      if (db.getSchema(TestOntologyTableIsGenerated.class.getSimpleName() + "2") != null) {
        db.dropSchema(TestOntologyTableIsGenerated.class.getSimpleName() + "2");
      }
      // delete this schema after other because of dependency
      s = db.dropCreateSchema(TestOntologyTableIsGenerated.class.getSimpleName());
      s.create(
          table(
              "test",
              column("name").setPkey(),
              column("code")
                  .setType(ColumnType.ONTOLOGY)
                  .setRefSchemaName(TestOntologyTableIsGenerated.class.getSimpleName() + "2")
                  .setRefTable("CodeTable")));
      fail("should fail if refSchema doesn't exit");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("does not exist"));
      // error correct
    }
    // create
    s.create(
        table(
            "test",
            column("name").setPkey(),
            column("code").setType(ColumnType.ONTOLOGY).setRefTable("CodeTable")));
    assertNotNull(s.getTable("CodeTable"));
    assertEquals(TableType.ONTOLOGIES, s.getTable("CodeTable").getMetadata().getTableType());

    // alter
    Table table = s.create(table("test_alter", column("name").setPkey(), column("code")));
    table
        .getMetadata()
        .alterColumn(column("code").setType(ColumnType.ONTOLOGY).setRefTable("CodeTable2"));

    assertNotNull(s.getTable("CodeTable2"));

    // external ontology table should reuse table
    s = db.dropCreateSchema(TestOntologyTableIsGenerated.class.getSimpleName() + "2");
    s.create(
        table(
            "test",
            column("name").setPkey(),
            column("code")
                .setType(ColumnType.ONTOLOGY)
                .setRefSchemaName(TestOntologyTableIsGenerated.class.getSimpleName())
                .setRefTable("CodeTable")));
    // should not create a table but use external one
    assertNull(s.getTable("CodeTable"));

    // external ontology table should create table
    s.create(
        table(
            "test_external",
            column("name").setPkey(),
            column("code")
                .setType(ColumnType.ONTOLOGY)
                .setRefSchemaName(TestOntologyTableIsGenerated.class.getSimpleName())
                .setRefTable("CodeTable3")));

    assertNotNull(
        db.getSchema(TestOntologyTableIsGenerated.class.getSimpleName()).getTable("CodeTable3"));

    // test simple ontology table creation
    String schema3 = TestOntologyTableIsGenerated.class.getSimpleName() + "3";
    s = db.dropCreateSchema(schema3);

    s.create(table("test_ontology").setTableType(TableType.ONTOLOGIES));
    assertNotNull(db.getSchema(schema3).getTable("test_ontology"));
    assertTrue(
        db.getSchema(schema3)
            .getTable("test_ontology")
            .getMetadata()
            .getColumnNames()
            .contains("name"));
  }
}
