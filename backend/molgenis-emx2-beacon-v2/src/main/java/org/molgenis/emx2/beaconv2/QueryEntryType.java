package org.molgenis.emx2.beaconv2;

import static org.molgenis.emx2.Privileges.AGGREGATOR;
import static org.molgenis.emx2.Privileges.VIEWER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.JsltException;
import com.schibsted.spt.data.jslt.Parser;
import graphql.ExecutionResult;
import graphql.GraphQL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;
import org.molgenis.emx2.beaconv2.requests.Filter;
import org.molgenis.emx2.graphql.GraphqlApiFactory;

public class QueryEntryType {

  public static JsonNode query(Database database, BeaconRequestBody requestBody)
      throws JsltException {
    EntryType entryType = requestBody.getQuery().getEntryType();

    ObjectMapper mapper = new ObjectMapper();
    ArrayNode resultSets = mapper.createArrayNode();
    ObjectNode response = mapper.createObjectNode();
    response.set("requestBody", mapper.valueToTree(requestBody));

    FilterParser filterParser = new FilterParser(requestBody.getQuery()).parse();
    if (filterParser.hasWarnings()) {
      ObjectNode info = mapper.createObjectNode();
      info.put("unsupportedFilters", filterParser.getWarnings().toString());
      response.set("info", info);
    }

    for (Table table : getTableFromAllSchemas(database, entryType.getId())) {
      if (isAuthorized(requestBody, table)) {
        JsonNode resultSet = doQuery(table, filterParser, requestBody);
        resultSets.add(resultSet);
      }
    }

    response.set("resultSets", resultSets);
    Expression jslt = Parser.compileResource(entryType.getName().toLowerCase() + ".jslt");
    return jslt.apply(response);
  }

  private static JsonNode doQuery(
      Table table, FilterParser filterParser, BeaconRequestBody requestBody) {
    GraphQL graphQL = new GraphqlApiFactory().createGraphqlForSchema(table.getSchema());

    String graphQlQuery =
        new QueryBuilder(table)
            .addAllColumns(2)
            .addFilters(filterParser.getGraphQlFilters())
            .setLimit(requestBody.getQuery().getPagination().getLimit())
            .setOffset(requestBody.getQuery().getPagination().getSkip())
            .getQuery();
    ExecutionResult result = graphQL.execute(graphQlQuery);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode results = mapper.valueToTree(result.getData());
    JsonNode entryTypeResult = results.get(requestBody.getQuery().getEntryType().getId());
    if (entryTypeResult == null || entryTypeResult.isNull()) return null;

    ArrayNode resultsArray = (ArrayNode) entryTypeResult;
    filterResults(resultsArray, filterParser.getPostFetchFilters());

    ObjectNode resultSet = mapper.createObjectNode();
    resultSet.put("id", table.getSchema().getName());
    resultSet.set("results", resultsArray);

    return resultSet;
  }

  private static boolean isAuthorized(BeaconRequestBody requestBody, Table table) {
    List<String> roles = table.getSchema().getInheritedRolesForActiveUser();
    switch (requestBody.getQuery().getRequestedGranularity()) {
      case BOOLEAN, COUNT, AGGREGATED:
        if (roles.contains(AGGREGATOR.toString())) return true;
      case RECORD, UNDEFINED:
        if (roles.contains(VIEWER.toString())) return true;
      default:
        return false;
    }
  }

  private static void filterResults(ArrayNode results, List<Filter> postFetchFilters) {
    postFetchFilters.forEach(
        filter -> {
          Iterator<JsonNode> resultsElements = results.elements();
          while (resultsElements.hasNext()) {
            JsonNode result = resultsElements.next();
            List<String> ageIso8601durations = new ArrayList<>();
            switch (filter.getConcept()) {
              case AGE_THIS_YEAR:
                ageIso8601durations.add(result.get("age_age_iso8601duration").textValue());
                break;
              case AGE_OF_ONSET:
                result
                    .get("diseases")
                    .forEach(
                        disease ->
                            ageIso8601durations.add(
                                disease.get("ageOfOnset_age_iso8601duration").textValue()));
                break;
              case AGE_AT_DIAG:
                result
                    .get("diseases")
                    .forEach(
                        disease ->
                            ageIso8601durations.add(
                                disease.get("ageAtDiagnosis_age_iso8601duration").textValue()));
                break;
            }
            if (!filter.filter(ageIso8601durations)) {
              resultsElements.remove();
            }
          }
        });
  }

  public static List<Table> getTableFromAllSchemas(Database database, String tableName) {
    List<Table> tables = new ArrayList<>();
    for (String sn : database.getSchemaNames()) {
      Schema schema = database.getSchema(sn);
      Table t = schema.getTable(tableName);
      if (t != null) {
        tables.add(t);
      }
    }
    return tables;
  }
}
