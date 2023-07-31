package org.molgenis.emx2.beaconv2.endpoints.filteringterms;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.*;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.TableMetadata;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class FilteringTermsResponse {

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private FilteringTerm[] filteringTerms;

  public FilteringTermsResponse(Database database) {
    this.filteringTerms = new FilteringTerm[] {};

    List<String> tableNames =
        Arrays.asList(
            "Analyses",
            "Biosamples",
            "Cohorts",
            "Dataset",
            "GenomicVariations",
            "Individuals",
            "Runs");

    Set<FilteringTerm> filteringTermsSet = new HashSet<>();

    for (String schemaName : database.getSchemaNames()) {
      for (String tableToQuery : tableNames) {
        Collection<String> tableNamesInSchema = database.getSchema(schemaName).getTableNames();
        if (tableNamesInSchema.contains(tableToQuery)) {
          TableMetadata metadata =
              database.getSchema(schemaName).getTable(tableToQuery).getMetadata();

          for (Column column : metadata.getColumns()) {
            if (!column.isReference() && !column.isOntology()) {
              FilteringTerm filteringTerm =
                  new FilteringTerm(column.getColumnType().name(), column.getName(), tableToQuery);
              filteringTermsSet.add(filteringTerm);
            }
          }
          // fix me: not a streaming implementation, could cause problem for big data sets ?
          List<Row> rows =
              database
                  .getSchema(schemaName)
                  .retrieveSql("Select distinct * from \"" + tableToQuery + "\"");

          for (Row row : rows) {
            for (Column columnPerRow : metadata.getColumns()) {
              if (columnPerRow.isPrimaryKey()
                  || (!columnPerRow.isReference() && !columnPerRow.isOntology())) {
                continue;
              }
              String colName = columnPerRow.getName();
              String value = row.getString(colName);
              if (value != null) {
                if (columnPerRow.isRefArray()) {
                  for (String valSplit : splitStringIgnoreQuotedCommas(value)) {
                    // to do: for ontologies, get the URI with an extra query as the 'id' value
                    FilteringTerm filteringTerm =
                        new FilteringTerm(
                            columnPerRow.getColumnType().name(), valSplit, valSplit, tableToQuery);
                    filteringTermsSet.add(filteringTerm);
                  }
                } else {
                  // to do: for ontologies, get the URI with an extra query as the 'id' value
                  FilteringTerm filteringTerm =
                      new FilteringTerm(
                          columnPerRow.getColumnType().name(), value, value, tableToQuery);
                  filteringTermsSet.add(filteringTerm);
                }
              }
            }
          }
        }
      }
    }

    this.filteringTerms = filteringTermsSet.toArray(new FilteringTerm[0]);
  }

  public static List<String> splitStringIgnoreQuotedCommas(String input) {
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

  public FilteringTerm[] getFilteringTerms() {
    return filteringTerms;
  }
}
