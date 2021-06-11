package org.molgenis.emx2.sql;

import static junit.framework.TestCase.*;

import junit.framework.TestCase;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;

public class TestMergeDrop {
  static Database db;
  static Schema schema;

  @BeforeClass
  public static void setup() {
    db = TestDatabaseFactory.getTestDatabase();
    schema = db.dropCreateSchema(TestMergeDrop.class.getSimpleName());
  }

  @Test
  public void testDrop() {
    SchemaMetadata newSchema = new SchemaMetadata();
    newSchema.create(TableMetadata.table("Person", Column.column("name")));

    schema.migrate(newSchema);
    // idempotent
    schema.migrate(newSchema);

    TestCase.assertNotNull(schema.getTable("Person"));

    newSchema = new SchemaMetadata();
    newSchema.create(TableMetadata.table("Person").drop());

    schema.migrate(newSchema);
    // should be idempotent
    schema.migrate(newSchema);

    TestCase.assertNull(schema.getTable("Person"));

    // now more complex, with dependency
    newSchema = new SchemaMetadata();
    newSchema.create(TableMetadata.table("Person", Column.column("name").setPkey()));
    newSchema.create(
        TableMetadata.table(
            "Pet",
            Column.column("name").setPkey(),
            Column.column("owner").setType(ColumnType.REF).setRefTable("Person")));

    schema.migrate(newSchema);
    // should be idempotent so repeat
    schema.migrate(newSchema);

    TestCase.assertNotNull(schema.getTable("Person"));
    TestCase.assertNotNull(schema.getTable("Pet"));

    schema.getTable("Person").insert(Row.row("name", "Donald"));
    schema.getTable("Pet").insert(Row.row("name", "Pluto", "owner", "Donald"));

    // should fail
    newSchema = new SchemaMetadata();
    try {
      newSchema.create(TableMetadata.table("Person").drop());
      schema.migrate(newSchema);
      fail("should fail because of foreign key");
    } catch (Exception e) {
      // fine
    }

    // should succeed
    newSchema.create(TableMetadata.table("Pet").drop());
    schema.migrate(newSchema);
    // should be idempotent so repeat
    schema.migrate(newSchema);

    TestCase.assertNull(schema.getTable("Person"));
    TestCase.assertNull(schema.getTable("Pet"));
  }
}
