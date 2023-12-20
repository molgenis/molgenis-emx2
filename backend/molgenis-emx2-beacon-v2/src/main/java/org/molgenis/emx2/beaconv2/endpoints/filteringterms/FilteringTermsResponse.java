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
          "Runs");

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
      // todo: now extended columns are ignored because make the query super complicated
      for (Column column : metadata.getLocalColumns()) {
        if (column.getColumnType().isAtomicType() && !column.getIdentifier().startsWith("mg_")) {
          FilteringTerm filteringTerm =
              new FilteringTerm("alphanumeric", column.getName(), tableToQuery);
          filteringTermsSet.add(filteringTerm);
        } else if (column.isOntology()) {
          String schema = metadata.getSchemaName();
          String refSchema = column.getRefTable().getSchemaName();
          List<Row> rows;
          String q =
              "SELECT DISTINCT name,codesystem,code FROM \""
                  + schema
                  + "\".\""
                  + tableToQuery
                  + "\" INNER JOIN \""
                  + refSchema
                  + "\".\""
                  + column.getRefTableName()
                  + "\" ON \""
                  + refSchema
                  + "\".\""
                  + column.getRefTableName()
                  + (column.isArray()
                      ? "\".\"name\" = ANY(\""
                          + schema
                          + "\".\""
                          + tableToQuery
                          + "\".\""
                          + column.getName()
                          + "\")"
                      : "\".\"name\" = \""
                          + schema
                          + "\".\""
                          + tableToQuery
                          + "\".\""
                          + column.getName()
                          + "\"");
          rows = database.getSchema(schemaName).retrieveSql(q);
          for (Row row : rows) {
            String codesystem = row.getString("codesystem");
            codesystem = codesystem == null || codesystem.isBlank() ? "NULL" : codesystem;
            String code = row.getString("code");
            code = code == null || code.isBlank() ? "NULL" : code;
            FilteringTerm filteringTerm =
                new FilteringTerm(
                    "ontology", codesystem + ":" + code, row.getString("name"), tableToQuery);
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
   * Getter for filteringTerms
   *
   * @return
   */
  public FilteringTerm[] getFilteringTerms() {
    return filteringTerms;
  }
}
