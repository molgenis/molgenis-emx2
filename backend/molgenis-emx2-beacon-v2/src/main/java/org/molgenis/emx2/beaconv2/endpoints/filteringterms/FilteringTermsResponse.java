package org.molgenis.emx2.beaconv2.endpoints.filteringterms;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.*;
import org.molgenis.emx2.*;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class FilteringTermsResponse {

  @JsonIgnore
  public static final List<String> BEACON_TABLES =
      Arrays.asList(
          "Analyses",
          "Biosamples",
          "Cohorts",
          "Dataset",
          "GenomicVariations",
          "Individuals",
          "SequencingRuns");

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private FilteringTerm[] filteringTerms;

  /**
   * From a database, get all schemas and add filtering terms to filteringTerms queried from all
   * tables in BEACON_TABLES
   *
   * @param database
   */
  public FilteringTermsResponse(Database database) {
    this.filteringTerms = new FilteringTerm[] {};
    Set<FilteringTerm> filteringTermsSet = new HashSet<>();
    for (String schemaName : database.getSchemaNames()) {
      getFilteringTermsFromTables(database, BEACON_TABLES, filteringTermsSet, schemaName);
    }
    this.filteringTerms = filteringTermsSet.toArray(new FilteringTerm[0]);
  }

  /**
   * Loop over all tables in a schema and query each for filtering terms
   *
   * @param database
   * @param tableNames
   * @param filteringTermsSet
   * @param schemaName
   */
  private void getFilteringTermsFromTables(
      Database database,
      List<String> tableNames,
      Set<FilteringTerm> filteringTermsSet,
      String schemaName) {
    for (String tableToQuery : tableNames) {
      Collection<String> tableNamesInSchema = database.getSchema(schemaName).getTableNames();
      getFilteringTermsFromOneTable(
          database, filteringTermsSet, schemaName, tableToQuery, tableNamesInSchema);
    }
  }

  /**
   * Check of a table is present in schema, add non-referencing terms immediately, launch native SQL
   * query for others and loop over rows
   *
   * @param database
   * @param filteringTermsSet
   * @param schemaName
   * @param tableToQuery
   * @param tableNamesInSchema
   */
  private void getFilteringTermsFromOneTable(
      Database database,
      Set<FilteringTerm> filteringTermsSet,
      String schemaName,
      String tableToQuery,
      Collection<String> tableNamesInSchema) {
    if (tableNamesInSchema.contains(tableToQuery)) {
      TableMetadata metadata = database.getSchema(schemaName).getTable(tableToQuery).getMetadata();
      for (Column column : metadata.getColumns()) {
        if (column.getColumnType().isAtomicType() && !column.getIdentifier().startsWith("mg_")) {
          FilteringTerm filteringTerm =
              new FilteringTerm("alphanumeric", column.getName(), tableToQuery);
          filteringTermsSet.add(filteringTerm);
        } else if (column.isOntology()) {
          List<Row> rows;
          if (column.isArray()) {
            rows =
                database
                    .getSchema(schemaName)
                    .retrieveSql(
                        "SELECT DISTINCT(name,codesystem,code) FROM \""
                            + tableToQuery
                            + "\" INNER JOIN \""
                            + column.getRefTableName()
                            + "\" ON \""
                            + column.getRefTableName()
                            + "\".\"name\" = ANY(\""
                            + column.getName()
                            + "\")");
          } else {
            rows =
                database
                    .getSchema(schemaName)
                    .retrieveSql(
                        "SELECT DISTINCT(name,codesystem,code) FROM \""
                            + tableToQuery
                            + "\" INNER JOIN \""
                            + column.getRefTableName()
                            + "\" ON \""
                            + column.getRefTableName()
                            + "\".\"name\" = \""
                            + column.getName()
                            + "\"");
          }

          for (Row row : rows) {
            List<String> rowData = parseRow(row);
            FilteringTerm filteringTerm =
                new FilteringTerm(
                    "ontology",
                    rowData.get(1) + "_" + rowData.get(2),
                    rowData.get(0),
                    tableToQuery);
            filteringTermsSet.add(filteringTerm);
          }
        } else {
          // ignore any non-atomic, non-ontology fields, which are headings, files and regular
          // (non-ontological) references
        }
      }
    }
  }

  /**
   * Helper function to parse raw data into list (e.g. ROW(row='+-----+---------+----+ |v0 |v1 |v2 |
   * +-----+---------+----+ |Dutch|HANCESTRO|0320| +-----+---------+----+' ) into {Dutch,
   * HANCESTRO,0320})
   *
   * @param row
   * @return
   */
  public List<String> parseRow(Row row) {
    List<String> result = new ArrayList<>();
    String[] splitRow = row.toString().split("\n");
    String rowData = splitRow[3];
    String[] splitValues = rowData.split("\\|");
    for (String value : splitValues) {
      String trimmedValue = value.trim();
      if (!trimmedValue.isBlank()) {
        result.add(trimmedValue);
      }
    }
    return result;
  }

  /**
   * Getter for filteringTerms
   *
   * @return
   */
  public FilteringTerm[] getFilteringTerms() {
    return filteringTerms;
  }
}
