package org.molgenis.emx2.sql;

import static graphql.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

public class TestHeadings {
  private static Database db;
  private static Schema schema;

  @BeforeAll
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
    schema = db.dropCreateSchema(TestHeadings.class.getSimpleName());
  }

  @Test
  void testSection() {
    Table table =
        schema.create(
            table(
                "mytable",
                column("my section").setType(ColumnType.SECTION),
                column("my heading").setType(ColumnType.HEADING),
                column("id").setType(ColumnType.STRING).setPkey()));

    table.insert(row("id", "my id"));
    Row result = table.retrieveRows().get(0);
    assertTrue(result.getColumnNames().contains("id"));
    assertFalse(result.getColumnNames().contains("my section"));
  }
}
