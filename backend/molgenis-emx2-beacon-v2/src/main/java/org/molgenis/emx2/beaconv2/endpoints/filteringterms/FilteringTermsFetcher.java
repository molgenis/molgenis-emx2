package org.molgenis.emx2.beaconv2.endpoints.filteringterms;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.beaconv2.EntryType;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class FilteringTermsFetcher {

  @JsonIgnore
  public static final List<String> BEACON_TABLES =
      Arrays.asList(
          EntryType.ANALYSES.getId(),
          EntryType.BIOSAMPLES.getId(),
          EntryType.COHORTS.getId(),
          // EntryType.DATASETS.getId(),
          EntryType.GENOMIC_VARIANT.getId(),
          EntryType.INDIVIDUALS.getId(),
          EntryType.RUNS.getId());

  @JsonIgnore private final Database database;

  /**
   * From a database, get all schemas and add filtering terms to filteringTerms queried from all
   * tables in BEACON_TABLES
   *
   * @param database
   */
  public FilteringTermsFetcher(Database database) {
    this.database = database;
  }

  public FilteringTerm[] getAllFilteringTerms() {
    return this.database.getSchemaNames().stream()
        .flatMap(schema -> getFilteringTermsFromTables(BEACON_TABLES, schema).stream())
        .toArray(FilteringTerm[]::new);
  }

  /**
   * Loop over all tables in a schema and query each for filtering terms
   *
   * @param tableNames
   * @param schemaName
   */
  public Set<FilteringTerm> getFilteringTermsFromTables(
      List<String> tableNames, String schemaName) {
    Set<FilteringTerm> filteringTerms = new HashSet<>();
    for (String tableToQuery : tableNames) {
      Collection<String> tableNamesInSchema = database.getSchema(schemaName).getTableNames();
      filteringTerms.addAll(
          getFilteringTermsFromOneTable(schemaName, tableToQuery, tableNamesInSchema));
    }
    return filteringTerms;
  }

  /**
   * Check of a table is present in schema, add non-referencing terms immediately, launch native SQL
   * query for others and loop over rows
   *
   * @param schemaName
   * @param tableToQuery
   * @param tableNamesInSchema
   */
  public Set<FilteringTerm> getFilteringTermsFromOneTable(
      String schemaName, String tableToQuery, Collection<String> tableNamesInSchema) {
    Set<FilteringTerm> filteringTerms = new HashSet<>();
    if (tableNamesInSchema.contains(tableToQuery)) {
      TableMetadata metadata = database.getSchema(schemaName).getTable(tableToQuery).getMetadata();
      // todo: now extended columns are ignored because make the query super complicated
      for (Column column : metadata.getLocalColumns()) {
        if (column.getColumnType().isAtomicType() && !column.getIdentifier().startsWith("mg_")) {
          FilteringTerm filteringTerm =
              new FilteringTerm("alphanumeric", column.getName(), tableToQuery);
          filteringTerms.add(filteringTerm);
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
                    column,
                    "ontology",
                    codesystem + ":" + code,
                    row.getString("name"),
                    tableToQuery);
            filteringTerms.add(filteringTerm);
          }
        } else {
          // ignore any non-atomic, non-ontology fields, which are headings, files and regular
          // (non-ontological) references
        }
      }
    }
    return filteringTerms;
  }
}
