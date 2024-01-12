package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
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

  @Test
  public void testSqlParameterized() {
    Schema schema = database.dropCreateSchema(TestSqlRawQueryForSchema.class.getSimpleName());
    new PetStoreLoader().load(schema, true);

    List<Row> rows =
        schema.retrieveSql("Select * from \"Pet\" p where p.name=${name}", Map.of("name", "spike"));
    assertEquals(1, rows.size());

    rows =
        schema.retrieveSql(
            "Select * from \"Pet\" p where p.name=ANY(${name:string_array})",
            Map.of("name", List.of("pooky", "spike")));
    assertEquals(2, rows.size());

    // error check
    try {
      schema.retrieveSql("Select * from \"Pet\" p where p.name=ANY(${blaat})", Map.of());
      fail("missing parameter should give proper error");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("Query expects parameter 'blaat'"));
    }

    try {
      schema.retrieveSql(
          "Select * from \"Pet\" p where p.name=ANY(${blaat:string_array})", Map.of());
      fail("missing parameter should give proper error");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("Query expects parameter 'blaat'"));
    }
  }
}
