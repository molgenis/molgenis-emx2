package org.molgenis.emx2.beaconv2.endpoints.filteringterms;

import static org.molgenis.emx2.SelectColumn.s;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.beaconv2.EntryType;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class FilteringTermsFetcher {

  @JsonIgnore private static final ObjectMapper MAPPER = new ObjectMapper();

  @JsonIgnore
  public static final List<String> BEACON_TABLES =
      Arrays.asList(
          EntryType.ANALYSES.getId(),
          // EntryType.BIOSAMPLES.getId(),
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
   * Check of a table is present in schema, add non-referencing terms immediately, query the terms
   * occurring in the data for ontology columns
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
          filteringTerms.addAll(getOntologyTerms(schemaName, tableToQuery, metadata, column));
        } else {
          // ignore any non-atomic, non-ontology fields, which are headings, files and regular
          // (non-ontological) references
        }
      }
    }
    return filteringTerms;
  }

  /**
   * Terms of an ontology column that occur in the data. Read through group by rather than a plain
   * select, so a caller holding only aggregate permissions still gets terms; group by needs Range,
   * tables below that are skipped instead of failing the whole response.
   */
  private Set<FilteringTerm> getOntologyTerms(
      String schemaName, String tableToQuery, TableMetadata metadata, Column column) {
    Schema schema = database.getSchema(schemaName);
    if (!PermissionEvaluator.canRange(schema, metadata)) {
      return Set.of();
    }
    Set<FilteringTerm> filteringTerms = new LinkedHashSet<>();
    String groupBy = tableToQuery + "_groupBy";
    String json =
        schema
            .query(groupBy, s("count"), s(column.getName(), s("name"), s("codesystem"), s("code")))
            .retrieveJSON();
    for (JsonNode group : readGroups(json, groupBy)) {
      JsonNode term = group.get(column.getIdentifier());
      if (term == null || term.isNull()) {
        continue;
      }
      filteringTerms.add(
          new FilteringTerm(
              column,
              "ontology",
              orNull(term, "codesystem") + ":" + orNull(term, "code"),
              text(term, "name"),
              tableToQuery));
    }
    return filteringTerms;
  }

  private JsonNode readGroups(String json, String groupBy) {
    try {
      JsonNode groups = MAPPER.readTree(json).get(groupBy);
      return groups == null || groups.isNull() ? MAPPER.createArrayNode() : groups;
    } catch (JsonProcessingException e) {
      throw new MolgenisException("Cannot read group by result of " + groupBy, e);
    }
  }

  private static String orNull(JsonNode node, String field) {
    String value = text(node, field);
    return value == null || value.isBlank() ? "NULL" : value;
  }

  private static String text(JsonNode node, String field) {
    JsonNode value = node.get(field);
    return value == null || value.isNull() ? null : value.asText();
  }
}
