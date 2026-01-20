package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.molgenis.emx2.FilterBean.f;

import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.DataModels;

class TestMatchNoneFilterQuery {

  private static Schema schema;

  @BeforeAll
  static void setup() {
    Database db = TestDatabaseFactory.getTestDatabase();
    String schemaName = TestMatchNoneFilterQuery.class.getSimpleName();
    db.dropSchemaIfExists(schemaName);
    DataModels.Profile.PET_STORE.getImportTask(db, schemaName, "", true).run();
    schema = db.getSchema(schemaName);
  }

  @Test
  void givenNoneMatchRed_thenNoRedPetsQueried() {
    List<Row> rows =
        schema.query("Pet").where(f("tags", f("name", Operator.MATCH_NONE, "red"))).retrieveRows();

    assertFalse(rows.isEmpty());

    boolean matches =
        rows.stream()
            .map(row -> (String) row.get("tags", ColumnType.STRING))
            .filter(Objects::nonNull)
            .anyMatch(tags -> tags.contains("red"));
    assertFalse(matches);
  }
}
