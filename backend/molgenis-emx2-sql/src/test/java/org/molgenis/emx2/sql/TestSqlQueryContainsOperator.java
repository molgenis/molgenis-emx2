package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.MATCH_ALL;
import static org.molgenis.emx2.Operator.MATCH_ANY;
import static org.molgenis.emx2.Row.row;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.DataModels;

public class TestSqlQueryContainsOperator {
  private static Database db;
  private static Schema schema;

  @BeforeAll
  public static void setUp() {
    db = new SqlDatabase(SqlDatabase.ADMIN_USER);
    schema = db.dropCreateSchema(TestSqlQueryContainsOperator.class.getSimpleName());
    DataModels.Profile.PET_STORE.getImportTask(schema, true).run();
    schema
        .getTable("Pet")
        .insert(row("name", "mickey", "photoUrls", "foo", "category", "dog", "weight", 1));
    schema
        .getTable("Pet")
        .insert(
            row(
                "name",
                "donald",
                "photoUrls",
                List.of("foo", "bar"),
                "category",
                "dog",
                "weight",
                1));
  }

  @Test
  void testContains() {
    // string array
    List<Row> result =
        schema
            .query("Pet")
            .where(f("photoUrls", MATCH_ANY, "aap", "noot", "mies", "foo"))
            .retrieveRows();
    assertEquals(2, result.size());
    result = schema.query("Pet").where(f("photoUrls", MATCH_ALL, "foo", "bar")).retrieveRows();
    assertEquals(1, result.size());

    // single column ref
    result = schema.query("Pet").where(f("tags", MATCH_ANY, "red", "green")).retrieveRows();
    assertEquals(5, result.size());

    result = schema.query("Pet").where(f("tags", MATCH_ALL, "red", "green")).retrieveRows();
    assertEquals(2, result.size());
  }
}
