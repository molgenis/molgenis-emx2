package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.field;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.*;
import static org.molgenis.emx2.Row.row;

import org.jooq.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.datamodels.DataModels;

public class TestOntologyQueries {

  private SqlSchema schema;

  @BeforeEach
  public void setUp() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = (SqlSchema) database.dropCreateSchema(TestOntologyQueries.class.getSimpleName());
    DataModels.Profile.PET_STORE.getImportTask(schema, true).run();
  }

  @Test
  void testGetTermsIncludingChildren() {
    // check that the function works
    Result<?> result =
        schema
            .getJooq()
            .select(
                field(
                    "\"MOLGENIS\".get_terms_including_children({0},{1},{2})",
                    schema.getName(), "Tag", new String[] {"colors"}))
            .fetch();
    assertTrue(result.toString().contains("colors"));
    assertTrue(result.toString().contains("green"));
    assertFalse(result.toString().contains("mammals"));

    // check that the function works
    result =
        schema
            .getJooq()
            .select(
                field(
                    "\"MOLGENIS\".get_terms_including_children({0},{1},{2})",
                    schema.getName(), "Tag", new String[] {"green", "blue"}))
            .fetch();
    assertTrue(result.toString().contains("green"));
    assertFalse(result.toString().contains("colors"));

    // expect 'colors' to return all colors
    String jsonResult =
        schema.query("Pet").where(f("tags", MATCH_ANY_INCLUDING_CHILDREN, "colors")).retrieveJSON();
    assertTrue(jsonResult.contains("spike")); // is 'red'
    assertFalse(jsonResult.contains("pooky")); // has no color

    // expect to work for normal colors too
    jsonResult =
        schema
            .query("Pet")
            .where(f("tags", MATCH_ANY_INCLUDING_CHILDREN, "purple", "blue"))
            .retrieveJSON();
    assertTrue(jsonResult.contains("sylvester")); // is 'purple'
    assertTrue(jsonResult.contains("jerry")); // is 'blue'
    assertFalse(jsonResult.contains("tom")); // is 'red'

    // expect to work for normal colors too
    jsonResult = schema.query("Pet").where(f("tags", MATCH_PATH, "purple", "blue")).retrieveJSON();
    assertTrue(jsonResult.contains("sylvester")); // is 'purple'
    assertTrue(jsonResult.contains("jerry")); // is 'blue'
    assertFalse(jsonResult.contains("tom")); // is 'red'

    // gave error elsewhere
    schema
        .query("Pet")
        .where(f("tags", MATCH_ANY_INCLUDING_CHILDREN, new String[] {"green", "blue"}))
        .retrieveJSON();
  }

  @Test
  void testGetTermsIncludingParents() {
    // check that the function works
    Result<?> result =
        schema
            .getJooq()
            .select(
                field(
                    "\"MOLGENIS\".get_terms_including_parents({0},{1},{2})",
                    schema.getName(), "Tag", new String[] {"red"}))
            .fetch();
    System.out.println(result);
    assertTrue(result.toString().contains("red"));
    assertTrue(result.toString().contains("colors"));
    assertFalse(result.toString().contains("mammals"));

    // check that the function works
    result =
        schema
            .getJooq()
            .select(
                field(
                    "\"MOLGENIS\".get_terms_including_parents({0},{1},{2})",
                    schema.getName(), "Tag", new String[] {"red", "mammals"}))
            .fetch();
    assertTrue(result.toString().contains("red"));
    assertTrue(result.toString().contains("colors"));
    assertTrue(result.toString().contains("mammals"));
    assertTrue(result.toString().contains("species"));
    assertFalse(result.toString().contains("carnivorous mammals"));

    // red (colors is not in the data)
    String jsonResult =
        schema.query("Pet").where(f("tags", MATCH_ANY_INCLUDING_PARENTS, "red")).retrieveJSON();
    assertTrue(jsonResult.contains("red"));

    // add some hierarchy
    schema
        .getTable("Pet")
        .insert(row("name", "extra", "tags", "colors", "category", "dog", "weight", 1));

    // mammals and thus also species (spike has species)
    jsonResult =
        schema.query("Pet").where(f("tags", MATCH_ANY_INCLUDING_PARENTS, "red")).retrieveJSON();
    assertTrue(jsonResult.contains("colors"));
    assertTrue(jsonResult.contains("extra"));
    assertFalse(result.toString().contains("pooky"));
  }
}
