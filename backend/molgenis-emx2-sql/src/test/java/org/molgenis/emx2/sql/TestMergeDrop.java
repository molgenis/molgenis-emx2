package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;

public class TestMergeDrop {
  static Database db;
  static Schema schema;

  @BeforeAll
  public static void setup() {
    db = TestDatabaseFactory.getTestDatabase();
    schema = db.dropCreateSchema(TestMergeDrop.class.getSimpleName());
  }

  @Test
  public void testDrop() {
    SchemaMetadata newSchema = new SchemaMetadata();
    newSchema.create(table("Person", column("name")));

    schema.migrate(newSchema);
    // idempotent
    schema.migrate(newSchema);

    assertNotNull(schema.getTable("Person"));

    newSchema = new SchemaMetadata();
    newSchema.create(table("Person"));
    newSchema.getTableMetadata("Person").drop();

    schema.migrate(newSchema);
    // should be idempotent
    schema.migrate(newSchema);

    assertNull(schema.getTable("Person"));

    // now more complex, with dependency
    newSchema = new SchemaMetadata();
    newSchema.create(table("Person", column("name").setPkey()));
    newSchema.create(
        table(
            "Pet",
            column("name").setPkey(),
            column("owner").setType(ColumnType.REF).setRefTable("Person")));

    schema.migrate(newSchema);
    // should be idempotent so repeat
    schema.migrate(newSchema);

    assertNotNull(schema.getTable("Person"));
    assertNotNull(schema.getTable("Pet"));

    schema.getTable("Person").insert(row("name", "Donald"));
    schema.getTable("Pet").insert(row("name", "Pluto", "owner", "Donald"));

    // should fail
    newSchema = new SchemaMetadata();
    try {
      newSchema.create(table("Person"));
      newSchema.getTableMetadata("Person").drop();
      schema.migrate(newSchema);
      fail("should fail because of foreign key");
    } catch (Exception e) {
      // fine
    }

    // should succeed
    newSchema.create(table("Pet"));
    newSchema.getTableMetadata("Pet").drop();
    schema.migrate(newSchema);
    // should be idempotent so repeat
    schema.migrate(newSchema);

    assertNull(schema.getTable("Person"));
    assertNull(schema.getTable("Pet"));
  }
}
