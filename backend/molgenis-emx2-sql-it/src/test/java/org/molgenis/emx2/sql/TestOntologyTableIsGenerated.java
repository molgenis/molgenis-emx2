package org.molgenis.emx2.sql;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

    // create
    s.create(
        table(
            "test",
            column("name").setPkey(),
            column("code").setType(ColumnType.ONTOLOGY).setRefTable("CodeTable")));
    assertNotNull(s.getTable("CodeTable"));

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
                .setRefSchema(TestOntologyTableIsGenerated.class.getSimpleName())
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
                .setRefSchema(TestOntologyTableIsGenerated.class.getSimpleName())
                .setRefTable("CodeTable3")));

    assertNotNull(
        db.getSchema(TestOntologyTableIsGenerated.class.getSimpleName()).getTable("CodeTable3"));
  }
}
