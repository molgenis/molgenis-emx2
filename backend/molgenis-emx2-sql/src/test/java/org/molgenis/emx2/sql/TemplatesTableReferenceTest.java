package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Constants.SYSTEM_SCHEMA;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;

class TemplatesTableReferenceTest {

  static Database db;
  static final String SCHEMA_NAME = "TemplatesTableReferenceTest";

  @BeforeAll
  static void setup() {
    db = TestDatabaseFactory.getTestDatabase();
    db.becomeAdmin();
    if (db.getSchema(SCHEMA_NAME) != null) {
      db.dropSchema(SCHEMA_NAME);
    }
    clearTemplatesFor(SCHEMA_NAME);
    Schema schema = db.createSchema(SCHEMA_NAME);
    schema.create(table("MyData", column("id").setPkey()));
  }

  @AfterAll
  static void cleanup() {
    db.becomeAdmin();
    clearTemplatesFor(SCHEMA_NAME);
    db.dropSchemaIfExists(SCHEMA_NAME);
  }

  private static void clearTemplatesFor(String schemaName) {
    Table templates = db.getSchema(SYSTEM_SCHEMA).getTable("Templates");
    List<Row> toDelete =
        templates.retrieveRows().stream()
            .filter(r -> schemaName.equals(r.getString("schema")))
            .toList();
    if (!toDelete.isEmpty()) {
      templates.delete(toDelete);
    }
  }

  private Table templates() {
    db.becomeAdmin();
    return db.getSchema(SYSTEM_SCHEMA).getTable("Templates");
  }

  @Test
  void templatesHasTableNameColumn() {
    assertTrue(templates().getMetadata().getColumnNames().contains("tableName"));
  }

  @Test
  void canBindEndpointToExistingTable() {
    int inserted =
        templates()
            .insert(
                row(
                    "endpoint", "beacon_individuals",
                    "schema", SCHEMA_NAME,
                    "tableName", "MyData"));
    assertEquals(1, inserted);
  }

  @Test
  void cannotBindEndpointToNonExistingTable() {
    Table templates = templates();
    Exception exception =
        assertThrows(
            Exception.class,
            () ->
                templates.insert(
                    row(
                        "endpoint", "beacon_cohorts",
                        "schema", SCHEMA_NAME,
                        "tableName", "DoesNotExist")));
    // foreign key violation against MOLGENIS.table_metadata
    assertNotNull(exception.getMessage());
  }
}
