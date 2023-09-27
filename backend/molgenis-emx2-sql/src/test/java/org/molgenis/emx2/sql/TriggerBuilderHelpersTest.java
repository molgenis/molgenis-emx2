package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TriggerBuilderHelpersTest {

  @Test
  void buildOntologySearchSectionForSingleValueType() {
    assertEquals(
        """
    SELECT
        "myTable_TEXT_SEARCH_COLUMN"
    FROM
        "mySchema"."myTable"
    WHERE
        "myTable"."name" = NEW."myColumn"
    """,
        TriggerBuilderHelpers.buildOntologySearchSection("mySchema", "myTable", "myColumn", false));
  }

  @Test
  void buildOntologySearchSectionForArrayType() {
    assertEquals(
        """
SELECT
    string_agg("myTable_TEXT_SEARCH_COLUMN", ' ')
FROM
    "mySchema"."myTable"
WHERE
    "myTable"."name" = ANY( NEW."myColumn")
""",
        TriggerBuilderHelpers.buildOntologySearchSection("mySchema", "myTable", "myColumn", true));
  }

  @Test
  void buildCallSearchTriggerFunction() {
    assertEquals(
        """
UPDATE "mySchema"."myTable" set "myTable_TEXT_SEARCH_COLUMN" = "myTable_TEXT_SEARCH_COLUMN";
""",
        TriggerBuilderHelpers.buildCallSearchTriggerFunction("mySchema", "myTable"));
  }
}
