package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;

public class TestReadonly {

  private static Schema schema;

  @BeforeAll
  public static void setUp() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(TestReadonly.class.getSimpleName());
  }

  @Test
  public void testReadonly() {
    Table table =
        schema.create(
            table(
                "TestReadonly",
                column("id").setPkey(),
                column("normal"),
                column("readonly").setReadonly(true)));

    table.insert(row("id", "1", "normal", "a", "readonly", "a"));
    table.update(row("id", "1", "normal", "b", "readonly", "b"));
    Row test = table.retrieveRows().get(0);
    assertEquals("b", test.getString("normal"));
    assertEquals("a", test.getString("readonly"));

    table.save(row("id", "1", "normal", "c", "readonly", "c"));
    test = table.retrieveRows().get(0);
    assertEquals("c", test.getString("normal"));
    assertEquals("a", test.getString("readonly"));
  }
}
