package org.molgenis.emx2.sql;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;

public class TestCreateDropColumn {

  static Database db;
  static Schema schema;

  @BeforeAll
  public static void setup() {
    db = TestDatabaseFactory.getTestDatabase();
    schema = db.dropCreateSchema(TestCreateDropColumn.class.getSimpleName());
  }

  @Test
  public void testCreateDropColumn() {
    Table table = schema.create(table("test", column("name").setPkey(), column("description")));
    table.insert(row("name", "a", "description", "an a"));
    table.getMetadata().dropColumn("description");
    table.insert(row("name", "b")); // regression #3499
  }
}
