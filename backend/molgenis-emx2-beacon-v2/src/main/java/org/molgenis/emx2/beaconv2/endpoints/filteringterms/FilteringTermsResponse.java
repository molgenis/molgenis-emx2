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
        if (column.getColumnType().isAtomicType()) {
          System.out.println("column.getColumnType() " + column.getColumnType() + " is atomic!");
          FilteringTerm filteringTerm =
              new FilteringTerm(
                  columnTypeToFilteringTermType(column.getColumnType()),
                  column.getName(),
                  tableToQuery);
          filteringTermsSet.add(filteringTerm);
        } else if (column.isOntology()) {
          System.out.println("column.getColumnType() " + column.getColumnType() + " is ontology!");
          List<Row> rows = null;
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
            System.out.println("ROW = " + row);
            System.out.println("get v0 = " + row.get("v0"));
            System.out.println("get v1 = " + row.getString("v1"));
            System.out.println("get v2 = " + row.getString("v2"));

            getFilteringTermsFromRow(filteringTermsSet, tableToQuery, metadata, row);
          }
        } else {

          System.out.println(
              "column.getColumnType() "
                  + column.getColumnType()
                  + " is NOT atomic and NOT ontology!");
        }
      }
    }
  }

  /**
   * Check referencing columns of a row for non-null values and add as filtering term
   *
   * @param filteringTermsSet
   * @param tableToQuery
   * @param metadata
   * @param row
   */
  private void getFilteringTermsFromRow(
      Set<FilteringTerm> filteringTermsSet, String tableToQuery, TableMetadata metadata, Row row) {
    for (Column columnPerRow : metadata.getColumns()) {
      if (columnPerRow.isPrimaryKey()
          || (!columnPerRow.isReference() && !columnPerRow.isOntology())) {
        continue;
      }
      String colName = columnPerRow.getName();
      String value = row.getString(colName);
      if (value != null) {
        addValueAsFilteringTerm(filteringTermsSet, tableToQuery, columnPerRow, value);
      }
    }
  }

  /**
   * Check if referencing value is an array and add as filtering term accordingly
   *
   * @param filteringTermsSet
   * @param tableToQuery
   * @param columnPerRow
   * @param value
   */
  private void addValueAsFilteringTerm(
      Set<FilteringTerm> filteringTermsSet,
      String tableToQuery,
      Column columnPerRow,
      String value) {
    if (columnPerRow.isRefArray()) {
      addValueArrayAsFilteringTerm(filteringTermsSet, tableToQuery, columnPerRow, value);
    } else {
      // to do: for ontologies, get the URI with an extra query as the 'id' value
      FilteringTerm filteringTerm =
          new FilteringTerm(
              columnTypeToFilteringTermType(columnPerRow.getColumnType()),
              value,
              value,
              tableToQuery);
      filteringTermsSet.add(filteringTerm);
    }
  }

  /**
   * Add array of referencing values as filtering terms by performing a smart CSV-aware split
   *
   * @param filteringTermsSet
   * @param tableToQuery
   * @param columnPerRow
   * @param value
   */
  private void addValueArrayAsFilteringTerm(
      Set<FilteringTerm> filteringTermsSet,
      String tableToQuery,
      Column columnPerRow,
      String value) {
    for (String valSplit : splitStringIgnoreQuotedCommas(value)) {
      // to do: for ontologies, get the URI with an extra query as the 'id' value
      FilteringTerm filteringTerm =
          new FilteringTerm(
              columnTypeToFilteringTermType(columnPerRow.getColumnType()),
              valSplit,
              valSplit,
              tableToQuery);
      filteringTermsSet.add(filteringTerm);
    }
  }

  /**
   * Helper function to perform smart CSV-aware split of input String representing a value array
   *
   * @param input
   * @return
   */
  public List<String> splitStringIgnoreQuotedCommas(String input) {
    List<String> tokens = new ArrayList<>();
    int startPosition = 0;
    boolean isInQuotes = false;
    for (int currentPosition = 0; currentPosition < input.length(); currentPosition++) {
      if (input.charAt(currentPosition) == '\"') {
        isInQuotes = !isInQuotes;
      } else if (input.charAt(currentPosition) == ',' && !isInQuotes) {
        String token = input.substring(startPosition, currentPosition);
        token =
            token.startsWith("\"") && token.endsWith("\"")
                ? token.substring(1, token.length() - 1)
                : token;
        tokens.add(token);
        startPosition = currentPosition + 1;
      }
    }
    String lastToken = input.substring(startPosition);
    if (lastToken.equals(",")) {
      tokens.add("");
    } else {
      lastToken =
          lastToken.startsWith("\"") && lastToken.endsWith("\"")
              ? lastToken.substring(1, lastToken.length() - 1)
              : lastToken;
      tokens.add(lastToken);
    }
    return tokens;
  }

  /**
   * Notes on mapping choices: Bool is not alphanumeric, althought it could be? File, UUID and
   * AUTO_ID are not meaningfully searchable. JSONB perhaps? REF and REFBACK are not alphanumeric
   * nor ontology. HEADING should be ignored.
   *
   * @param columnType
   * @return
   */
  public String columnTypeToFilteringTermType(ColumnType columnType) {

    switch (columnType) {
      case ONTOLOGY:
      case ONTOLOGY_ARRAY:
        return "ontology";
      case STRING:
      case STRING_ARRAY:
      case TEXT:
      case TEXT_ARRAY:
      case INT:
      case INT_ARRAY:
      case LONG:
      case LONG_ARRAY:
      case DECIMAL:
      case DECIMAL_ARRAY:
      case DATE:
      case DATE_ARRAY:
      case DATETIME:
      case DATETIME_ARRAY:
      case EMAIL:
      case EMAIL_ARRAY:
      case HYPERLINK:
      case HYPERLINK_ARRAY:
        return "alphanumeric";
      default:
        return "custom";
    }
  }

  /**
   * Getter
   *
   * @return
   */
  public FilteringTerm[] getFilteringTerms() {
    return filteringTerms;
  }
}
