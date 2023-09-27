package org.molgenis.emx2.sql;

public class TriggerBuilderHelpers {

  private TriggerBuilderHelpers() {
    throw new IllegalStateException("Utility class");
  }

  public static String buildOntologySearchSection(
      String schemaName, String tableName, String columnName, boolean isArray) {
    String template =
        isArray
            ? """
                SELECT
                    string_agg("%2$s_TEXT_SEARCH_COLUMN", ' ')
                FROM
                    "%1$s"."%2$s"
                WHERE
                    "%2$s"."name" = ANY( NEW."%3$s")
                """
            : """
                SELECT
                    "%2$s_TEXT_SEARCH_COLUMN"
                FROM
                    "%1$s"."%2$s"
                WHERE
                    "%2$s"."name" = NEW."%3$s"
                """;
    return template.formatted(schemaName, tableName, columnName);
  }
}
