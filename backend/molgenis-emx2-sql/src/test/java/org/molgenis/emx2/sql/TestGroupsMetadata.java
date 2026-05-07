package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.REF_ARRAY;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;

public class TestGroupsMetadata {

  private static final String SCHEMA_NAME = TestGroupsMetadata.class.getSimpleName();
  private static Database db;
  private static DSLContext jooq;

  @BeforeAll
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
    jooq = ((SqlDatabase) db).getJooq();
  }

  @AfterAll
  public static void tearDown() {
    jooq.execute("DELETE FROM \"MOLGENIS\".groups_metadata WHERE schema LIKE ?", SCHEMA_NAME + "%");
    db.dropSchemaIfExists(SCHEMA_NAME + "Gin");
    db.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  public void groupsMetadataTableHasCorrectStructure() {
    org.jooq.Schema molgenisSchema = jooq.meta().getSchemas("MOLGENIS").get(0);
    org.jooq.Table<?> groupsTable =
        molgenisSchema.getTables().stream()
            .filter(t -> "groups_metadata".equals(t.getName()))
            .findFirst()
            .orElse(null);
    assertNotNull(groupsTable, "MOLGENIS.groups_metadata must exist after DB init");

    assertNotNull(groupsTable.field("schema"), "column 'schema' must exist");
    assertNotNull(groupsTable.field("name"), "column 'name' must exist");
    assertNotNull(groupsTable.field("users"), "column 'users' must exist");

    List<?> primaryKeys = groupsTable.getKeys();
    assertFalse(primaryKeys.isEmpty(), "groups_metadata must have a primary key");

    List<Record> fkRecords =
        jooq.fetch(
            "SELECT tc.constraint_name"
                + " FROM information_schema.table_constraints tc"
                + " JOIN information_schema.referential_constraints rc"
                + "   ON tc.constraint_name = rc.constraint_name"
                + " WHERE tc.table_schema = 'MOLGENIS'"
                + "   AND tc.table_name = 'groups_metadata'"
                + "   AND tc.constraint_type = 'FOREIGN KEY'");
    assertFalse(fkRecords.isEmpty(), "groups_metadata must have a FK to schema_metadata");
  }

  @Test
  public void groupsMetadataFkCascadesOnSchemaDelete() {
    Schema schema = db.dropCreateSchema(SCHEMA_NAME);
    String schemaName = schema.getName();

    jooq.execute(
        "INSERT INTO \"MOLGENIS\".groups_metadata (schema, name) VALUES (?, ?)",
        schemaName,
        "group-alpha");

    assertThrows(
        Exception.class,
        () ->
            jooq.execute(
                "INSERT INTO \"MOLGENIS\".groups_metadata (schema, name) VALUES (?, ?)",
                "nonexistent-schema-xyz",
                "group-bogus"),
        "insert with non-existent schema must fail due to FK constraint");

    List<Record> before =
        jooq.fetch("SELECT name FROM \"MOLGENIS\".groups_metadata WHERE schema = ?", schemaName);
    assertEquals(1, before.size(), "group must exist before schema drop");

    db.dropSchema(schemaName);

    List<Record> after =
        jooq.fetch("SELECT name FROM \"MOLGENIS\".groups_metadata WHERE schema = ?", schemaName);
    assertEquals(0, after.size(), "groups must be cascade-deleted when schema is dropped");
  }

  @Test
  public void refArrayColumnHasGinIndex() {
    String ginSchemaName = SCHEMA_NAME + "Gin";
    db.dropSchemaIfExists(ginSchemaName);
    Schema schema = db.createSchema(ginSchemaName);

    schema.create(table("Groups", column("name").setPkey()));
    schema.create(
        table(
            "Things",
            column("id").setPkey(),
            column("groups").setType(REF_ARRAY).setRefTable("Groups")));

    List<Record> indexes =
        jooq.fetch(
            "SELECT indexname FROM pg_indexes WHERE schemaname = ? AND tablename = ? AND indexname = ?",
            ginSchemaName,
            "Things",
            "Things/groups");
    assertFalse(indexes.isEmpty(), "GIN index 'Things/groups' must exist on Things.groups");
  }
}
