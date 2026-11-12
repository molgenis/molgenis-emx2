package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Constants.SYSTEM_SCHEMA;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import org.jooq.Record;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;

class TemplatesTableReferenceTest {

  static Database db;
  private static final String SCHEMA_NAME = TemplatesTableReferenceTest.class.getSimpleName();

  @BeforeAll
  static void setup() {
    db = TestDatabaseFactory.getTestDatabase();
    db.becomeAdmin();
    Schema schema = db.dropCreateSchema(SCHEMA_NAME);
    schema.create(table("MyData", column("id").setPkey()));
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
  void templatesTableNameFkeyIsACascadingForeignKeyToTableMetadata() {
    // CI gives every job a fresh postgres, so init() already ran addTemplatesTableReference:
    // reading this constraint's shape tests that helper end to end, without mutating shared state.
    db.becomeAdmin();
    SqlDatabase sqlDb = (SqlDatabase) db;
    Record constraint = fetchTemplatesTableNameFkey(sqlDb);

    assertNotNull(constraint, "Templates_tableName_fkey must exist on _SYSTEM_.Templates");
    assertEquals("f", constraint.get("contype", String.class), "must be a FOREIGN KEY constraint");
    assertArrayEquals(
        new String[] {"schema", "tableName"},
        constraint.get("local_columns", String[].class),
        "must be on columns schema, tableName in that order");
    assertArrayEquals(
        new String[] {"table_schema", "table_name"},
        constraint.get("foreign_columns", String[].class),
        "must reference table_schema, table_name in that order");
    assertEquals(
        "\"MOLGENIS\".table_metadata",
        constraint.get("foreign_table", String.class),
        "must reference MOLGENIS.table_metadata");
    assertEquals("c", constraint.get("confupdtype", String.class), "must be ON UPDATE CASCADE");
    assertEquals("c", constraint.get("confdeltype", String.class), "must be ON DELETE CASCADE");
  }

  private static Record fetchTemplatesTableNameFkey(SqlDatabase sqlDb) {
    return sqlDb
        .getJooq()
        .fetchOne(
            "SELECT con.contype, con.confupdtype, con.confdeltype,"
                + " con.confrelid::regclass::text AS foreign_table,"
                + " array_agg(att.attname ORDER BY k.ord) AS local_columns,"
                + " array_agg(fatt.attname ORDER BY k.ord) AS foreign_columns"
                + " FROM pg_constraint con"
                + " JOIN LATERAL unnest(con.conkey, con.confkey) WITH ORDINALITY AS k(attnum,"
                + " fattnum, ord) ON true"
                + " JOIN pg_attribute att ON att.attrelid = con.conrelid AND att.attnum ="
                + " k.attnum"
                + " JOIN pg_attribute fatt ON fatt.attrelid = con.confrelid AND fatt.attnum ="
                + " k.fattnum"
                + " WHERE con.conname = 'Templates_tableName_fkey'"
                + " AND con.conrelid = '\"_SYSTEM_\".\"Templates\"'::regclass"
                + " GROUP BY con.contype, con.confrelid, con.confupdtype, con.confdeltype");
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
