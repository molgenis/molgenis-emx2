package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.INT;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;

public class TestTableRename {

  private static Database db;

  @BeforeAll
  static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  void renamedTableStillAcceptsInsertsAndReads() {
    String schemaName = TestTableRename.class.getSimpleName() + "_plain";
    Schema schema = db.dropCreateSchema(schemaName);
    schema.create(table("Plain", column("name").setPkey(), column("size").setType(INT)));
    schema.getTable("Plain").insert(row("name", "before", "size", 1));

    schema.getMetadata().getTableMetadata("Plain").alterName("PlainRenamed");

    db.clearCache();
    Schema reloaded = db.getSchema(schemaName);
    assertNotNull(reloaded.getMetadata().getTableMetadata("PlainRenamed"));
    reloaded.getTable("PlainRenamed").insert(row("name", "after", "size", 2));
    assertEquals(2, reloaded.getTable("PlainRenamed").retrieveRows().size());
  }

  @Test
  void renamedInheritingChildStillAcceptsInsertsAndReads() {
    String schemaName = TestTableRename.class.getSimpleName() + "_child";
    Schema schema = db.dropCreateSchema(schemaName);
    schema.create(table("Shape", column("name").setPkey()));
    schema.create(table("MyShape", column("size").setType(INT)).setInheritName("Shape"));
    schema.getTable("MyShape").insert(row("name", "before", "size", 1));

    schema.getMetadata().getTableMetadata("MyShape").alterName("MyRenamedShape");

    db.clearCache();
    Schema reloaded = db.getSchema(schemaName);
    assertNotNull(reloaded.getMetadata().getTableMetadata("MyRenamedShape"));
    reloaded.getTable("MyRenamedShape").insert(row("name", "after", "size", 2));
    assertEquals(2, reloaded.getTable("MyRenamedShape").retrieveRows().size());
  }

  @Test
  void renamedTableStillSearchable() {
    String schemaName = TestTableRename.class.getSimpleName() + "_search";
    Schema schema = db.dropCreateSchema(schemaName);
    schema.create(table("Plain", column("name").setPkey(), column("size").setType(INT)));

    schema.getMetadata().getTableMetadata("Plain").alterName("PlainRenamed");

    db.clearCache();
    Schema reloaded = db.getSchema(schemaName);
    reloaded.getTable("PlainRenamed").insert(row("name", "findme", "size", 1));
    assertEquals(
        1, reloaded.getTable("PlainRenamed").query().search("findme").retrieveRows().size());
  }
}
