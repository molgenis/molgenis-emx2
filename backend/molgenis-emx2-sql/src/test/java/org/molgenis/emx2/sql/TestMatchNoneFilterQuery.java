package org.molgenis.emx2.sql;

import static org.molgenis.emx2.FilterBean.f;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Operator;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.DataModels;

class TestMatchNoneFilterQuery {

  private static Database db;
  private static Schema schema;

  @BeforeAll
  static void setup() {
    db = TestDatabaseFactory.getTestDatabase();
    schema = db.dropCreateSchema(TestMatchNoneFilterQuery.class.getSimpleName());
    DataModels.Profile.PET_STORE.getImportTask(schema, true).run();
  }

  @Test
  void givenNoneMatchRed_thenNoRedPetsQueried() {
    List<Row> rows =
        schema.query("Pet").where(f("tags", Operator.MATCH_NONE, "red")).retrieveRows();
  }
}
