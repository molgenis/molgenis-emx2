package org.molgenis.emx2.beaconv2;

import static org.molgenis.emx2.Privileges.AGGREGATOR;
import static org.molgenis.emx2.Privileges.VIEWER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
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
import org.molgenis.emx2.beaconv2.common.misc.Granularity;
import org.molgenis.emx2.beaconv2.common.misc.IncludedResultsetResponses;
import org.molgenis.emx2.beaconv2.filter.Filter;
import org.molgenis.emx2.beaconv2.filter.FilterParser;
import org.molgenis.emx2.beaconv2.filter.FilterParserFactory;
import org.molgenis.emx2.beaconv2.requests.BeaconQuery;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;
import org.molgenis.emx2.graphql.GraphqlApiFactory;

public class QueryEntryType {

  private static final ObjectMapper mapper = new ObjectMapper();
  public static final int MAX_QUERY_DEPTH = 3;

  public static JsonNode query(Database database, BeaconRequestBody request) throws JsltException {
    EntryType entryType = request.getQuery().getEntryType();
    Granularity granularity = request.getQuery().getRequestedGranularity();
    IncludedResultsetResponses includeStrategy = request.getQuery().getIncludeResultsetResponses();

    ObjectNode response = mapper.createObjectNode();
    response.set("requestBody", mapper.valueToTree(request));
    FilterParser filterParser = parseFilters(request, response);

    int numTotalResults = 0;
    ArrayNode resultSets = mapper.createArrayNode();
    for (Table table : getTableFromAllSchemas(database, entryType.getId())) {
      if (isAuthorized(request, table)) {
        ObjectNode resultSet = mapper.createObjectNode();
        resultSet.put("id", table.getSchema().getName());

        ArrayNode resultsArray = doGraphQlQuery(table, filterParser, request);
        filterResults(resultsArray, filterParser.getPostFetchFilters());
        if (hasResult(resultsArray)) {
          numTotalResults += resultsArray.size();
          switch (granularity) {
            case RECORD, UNDEFINED:
              ArrayNode paginatedResults = paginateResults(resultsArray, request.getQuery());
              resultSet.set("results", paginatedResults);
            case COUNT, AGGREGATED:
              resultSet.put("count", resultsArray.size());
          }
          resultSets.add(resultSet);
        } else if (includeStrategy.equals(IncludedResultsetResponses.ALL)) {
          resultSets.add(resultSet);
        }
      }
    }
    if (!granularity.equals(Granularity.BOOLEAN)) {
      response.put("numTotalResults", numTotalResults);
    }
    response.set("resultSets", resultSets);
    return getJsltResponse(response, request);
  }

  private static ObjectNode getJsltResponse(ObjectNode response, BeaconRequestBody request) {
    EntryType entryType = request.getQuery().getEntryType();
    Granularity granularity = request.getQuery().getRequestedGranularity();
    ArrayNode resultSets = response.withArray("resultSets");
    Expression jslt =
        Parser.compileResource("entry-types/" + entryType.getName().toLowerCase() + ".jslt");

    ObjectNode jsltResponse = (ObjectNode) jslt.apply(response);

    if (granularity.equals(Granularity.RECORD) && resultSets.isEmpty()) {
      addEmptyResultSet(entryType, jsltResponse);
    }

    return jsltResponse;
  }

  private static void addEmptyResultSet(EntryType entryType, ObjectNode jsltResponse) {
    switch (entryType) {
      case COHORTS, DATASETS:
        jsltResponse.set(
            "response", mapper.createObjectNode().set("collections", mapper.createArrayNode()));
        break;
      default:
        jsltResponse.set(
            "response", mapper.createObjectNode().set("resultSets", mapper.createArrayNode()));
    }
  }

  private static boolean hasResult(ArrayNode resultsArray) {
    return resultsArray != null && !resultsArray.isNull() && !resultsArray.isEmpty();
  }

  private static FilterParser parseFilters(BeaconRequestBody request, ObjectNode response) {
    FilterParser filterParser = FilterParserFactory.getParserForRequest(request).parse();
    if (filterParser.hasWarnings()) {
      ObjectNode info = mapper.createObjectNode();
      info.put("unsupportedFilters", filterParser.getWarnings().toString());
      response.set("info", info);
    }
    return filterParser;
  }

  private static ArrayNode doGraphQlQuery(
      Table table, FilterParser filterParser, BeaconRequestBody requestBody) {
    GraphQL graphQL = new GraphqlApiFactory().createGraphqlForSchema(table.getSchema());

    String graphQlQuery =
        new QueryBuilder(table)
            .addAllColumns(MAX_QUERY_DEPTH)
            .addFilters(filterParser.getGraphQlFilters())
            .getQuery();
    ExecutionResult result = graphQL.execute(graphQlQuery);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode results = mapper.valueToTree(result.getData());
    JsonNode entryTypeResult = results.get(requestBody.getQuery().getEntryType().getId());
    if (entryTypeResult == null || entryTypeResult.isNull()) return null;

    return (ArrayNode) entryTypeResult;
  }

  private static ArrayNode paginateResults(ArrayNode results, BeaconQuery query) {
    if (results == null || results.isNull()) return null;

    int skip = query.getPagination().getSkip();
    int limit = query.getPagination().getLimit();

    ArrayNode paginatedResults = JsonNodeFactory.instance.arrayNode();
    for (int i = skip; i < Math.min(skip + limit, results.size()); i++) {
      paginatedResults.add(results.get(i));
    }
    return paginatedResults;
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

  // todo: move some code to Filter class
  private static void filterResults(ArrayNode results, List<Filter> postFetchFilters) {
    if (!hasResult(results)) return;
    for (Filter filter : postFetchFilters) {
      Iterator<JsonNode> resultsElements = results.elements();
      while (resultsElements.hasNext()) {
        JsonNode result = resultsElements.next();
        List<String> ageIso8601durations = new ArrayList<>();
        switch (filter.getConcept()) {
          case AGE_THIS_YEAR:
            ageIso8601durations.add(result.get("age_age_iso8601duration").textValue());
            break;
          case AGE_OF_ONSET:
            for (JsonNode disease : result.get("diseases")) {
              String age = disease.get("ageOfOnset_age_iso8601duration").textValue();
              ageIso8601durations.add(age);
            }
            break;
          case AGE_AT_DIAG:
            for (JsonNode disease : result.get("diseases")) {
              String age = disease.get("ageAtDiagnosis_age_iso8601duration").textValue();
              ageIso8601durations.add(age);
            }
            break;
        }
        if (!filter.filter(ageIso8601durations)) {
          resultsElements.remove();
        }
      }
    }
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
