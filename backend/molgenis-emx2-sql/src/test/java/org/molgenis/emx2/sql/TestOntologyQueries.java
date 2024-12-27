package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.field;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.MATCH_ANY_IN_SUBTREE;

import org.jooq.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.datamodels.PetStoreLoader;

public class TestOntologyQueries {

  private SqlSchema schema;

  @BeforeEach
  public void setUp() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = (SqlSchema) database.dropCreateSchema(TestOntologyQueries.class.getSimpleName());
    new PetStoreLoader(schema, true).run();
  }

  @Test
  void testSubtreeFilter() {
    // check that the function works
    Result<?> result =
        schema
            .getJooq()
            .select(
                field(
                    "\"MOLGENIS\".get_expanded_ontology_terms({0},{1},{2})",
                    schema.getName(), "Tag", new String[] {"colors"}))
            .fetch();
    assertTrue(result.toString().contains("green"));

    // expect 'colors' to return all colors
    String jsonResult =
        schema.query("Pet").where(f("tags", MATCH_ANY_IN_SUBTREE, "colors")).retrieveJSON();
    assertTrue(jsonResult.contains("spike")); // is 'red'
    assertFalse(jsonResult.contains("pooky")); // has no color

    // expect to work for normal colors too
    jsonResult =
        schema.query("Pet").where(f("tags", MATCH_ANY_IN_SUBTREE, "purple", "blue")).retrieveJSON();
    assertTrue(jsonResult.contains("sylvester")); // is 'purple'
    assertTrue(jsonResult.contains("jerry")); // is 'blue'
    assertFalse(jsonResult.contains("tom")); // is 'red'
  }
}
