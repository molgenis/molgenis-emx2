package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.PetStoreLoader;

public class TestSqlRawQueryForSchema {

  private static Database database;

  @BeforeAll
  public static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testSql() {
    Schema schema = database.dropCreateSchema(TestSqlRawQueryForSchema.class.getSimpleName());
    new PetStoreLoader().load(schema, true);
    List<Row> rows = schema.retrieveSql("Select * from \"Pet\"");
    assertEquals(8, rows.size());
  }
}
