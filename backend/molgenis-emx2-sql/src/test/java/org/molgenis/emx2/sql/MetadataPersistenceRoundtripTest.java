package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

class MetadataPersistenceRoundtripTest {

  private static final String SCHEMA_NAME = MetadataPersistenceRoundtripTest.class.getSimpleName();
  private static Database db;

  @BeforeAll
  static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
    db.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  void enumColumnValuesRoundtrip() {
    Schema schema = db.dropCreateSchema(SCHEMA_NAME + "Enum");

    List<String> allowedValues = List.of("alpha", "beta", "gamma");
    schema.create(
        table("Choices")
            .add(column("id").setPkey())
            .add(column("category").setType(ENUM).setValues(allowedValues)));

    db.clearCache();
    Schema reloaded = db.getSchema(SCHEMA_NAME + "Enum");
    Column reloadedColumn = reloaded.getTable("Choices").getMetadata().getColumn("category");

    assertNotNull(reloadedColumn, "column 'category' must survive reload");
    assertEquals(ENUM, reloadedColumn.getColumnType());
    assertEquals(allowedValues, reloadedColumn.getValues(), "values must round-trip");
  }

  @Test
  void subclassColumnValuesRoundtrip() {
    Schema schema = db.dropCreateSchema(SCHEMA_NAME + "Sub");

    List<String> subtypeValues = List.of("TypeA", "TypeB");
    schema.create(
        table("Root")
            .add(column("id").setPkey())
            .add(column("kind").setType(SUBCLASS).setValues(subtypeValues)));

    db.clearCache();
    Schema reloaded = db.getSchema(SCHEMA_NAME + "Sub");
    Column reloadedColumn = reloaded.getTable("Root").getMetadata().getColumn("kind");

    assertNotNull(reloadedColumn, "column 'kind' must survive reload");
    assertEquals(SUBCLASS, reloadedColumn.getColumnType());
    assertEquals(subtypeValues, reloadedColumn.getValues(), "SUBCLASS values must round-trip");
  }

  @Test
  void inheritNamesRoundtripSingleParent() {
    Schema schema = db.dropCreateSchema(SCHEMA_NAME + "Inh");

    schema.create(table("Animal").add(column("name").setPkey()));
    schema.create(table("Dog").setInheritNames("Animal"));

    db.clearCache();
    Schema reloaded = db.getSchema(SCHEMA_NAME + "Inh");
    TableMetadata dogMeta = reloaded.getTable("Dog").getMetadata();

    assertEquals(List.of("Animal"), dogMeta.getInheritNames(), "inheritNames list must round-trip");
    assertEquals(List.of("Animal"), dogMeta.getInheritNames(), "inheritNames list must round-trip");
  }

  @Test
  void noValuesColumnRoundtripsAsNull() {
    Schema schema = db.dropCreateSchema(SCHEMA_NAME + "Null");

    schema.create(table("Simple").add(column("id").setPkey()).add(column("label")));

    db.clearCache();
    Schema reloaded = db.getSchema(SCHEMA_NAME + "Null");
    Column reloadedColumn = reloaded.getTable("Simple").getMetadata().getColumn("label");

    assertNotNull(reloadedColumn, "column 'label' must survive reload");
    assertNull(reloadedColumn.getValues(), "values must be null when not set");
  }
}
