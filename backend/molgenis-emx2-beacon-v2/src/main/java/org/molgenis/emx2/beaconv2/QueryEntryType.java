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
import java.util.List;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.beaconv2.common.misc.Granularity;
import org.molgenis.emx2.beaconv2.common.misc.IncludedResultsetResponses;
import org.molgenis.emx2.beaconv2.filter.FilterParser;
import org.molgenis.emx2.beaconv2.filter.FilterParserFactory;
import org.molgenis.emx2.beaconv2.requests.BeaconQuery;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;
import org.molgenis.emx2.graphql.GraphqlApiFactory;

public class QueryEntryType {

  private static final int MAX_QUERY_DEPTH = 2;

  private final BeaconRequestBody request;
  private final BeaconQuery beaconQuery;
  private final EntryType entryType;
  private final Granularity granularity;
  private final IncludedResultsetResponses includeStrategy;

  private final ObjectMapper mapper = new ObjectMapper();

  public QueryEntryType(BeaconRequestBody request) {
    this.request = request;
    this.beaconQuery = request.getQuery();
    this.entryType = request.getQuery().getEntryType();
    this.granularity = request.getQuery().getRequestedGranularity();
    this.includeStrategy = request.getQuery().getIncludeResultsetResponses();
  }

  public JsonNode query(Database database) throws JsltException {
    ObjectNode response = mapper.createObjectNode();
    response.set("requestBody", mapper.valueToTree(request));
    FilterParser filterParser = parseFilters(response);

    int numTotalResults = 0;
    ArrayNode resultSets = mapper.createArrayNode();

    List<Table> entryTypeTables = database.getTablesFromAllSchemas(entryType.getId());
    for (Table table : entryTypeTables) {
      if (isAuthorized(table)) {
        ObjectNode resultSet = mapper.createObjectNode();
        resultSet.put("id", table.getSchema().getName());

        switch (granularity) {
          case RECORD, UNDEFINED:
            ArrayNode resultsArray = doGraphQlQuery(table, filterParser.getGraphQlFilters());
            if (hasResult(resultsArray)) {
              numTotalResults += resultsArray.size();
              ArrayNode paginatedResults = paginateResults(resultsArray);
              resultSet.set("results", paginatedResults);
              resultSet.put("count", resultsArray.size());
              resultSets.add(resultSet);
            } else if (includeStrategy.equals(IncludedResultsetResponses.ALL)) {
              resultSets.add(resultSet);
            }
            break;
          case BOOLEAN, COUNT, AGGREGATED:
            int count = doCountQuery(table, filterParser.getGraphQlFilters());
            if (count > 0) {
              resultSet.put("exist", true);
              numTotalResults += count;
              if (!granularity.equals(Granularity.BOOLEAN)) {
                resultSet.put("count", count);
              }
              resultSets.add(resultSet);
            } else if (includeStrategy.equals(IncludedResultsetResponses.ALL)) {
              resultSets.add(resultSet);
            }
        }
      }
    }
    if (!granularity.equals(Granularity.BOOLEAN)) {
      response.put("numTotalResults", numTotalResults);
    }
    response.set("resultSets", resultSets);
    return getJsltResponse(response);
  }

  private ObjectNode getJsltResponse(ObjectNode response) {
    ArrayNode resultSets = response.withArray("resultSets");

    String jsltPath = "entry-types/" + entryType.getName().toLowerCase() + ".jslt";
    Expression jslt = Parser.compileResource(jsltPath);
    ObjectNode jsltResponse = (ObjectNode) jslt.apply(response);

    if (granularity.equals(Granularity.RECORD) && resultSets.isEmpty()) {
      addEmptyResultSet(jsltResponse);
    }

    return jsltResponse;
  }

  private void addEmptyResultSet(ObjectNode jsltResponse) {
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

  private boolean hasResult(ArrayNode resultsArray) {
    return resultsArray != null && !resultsArray.isNull() && !resultsArray.isEmpty();
  }

  private FilterParser parseFilters(ObjectNode response) {
    FilterParser filterParser = FilterParserFactory.getParserForRequest(request).parse();
    if (filterParser.hasWarnings()) {
      ObjectNode info = mapper.createObjectNode();
      info.put("unsupportedFilters", filterParser.getWarnings().toString());
      response.set("info", info);
    }
    return filterParser;
  }

  private ArrayNode doGraphQlQuery(Table table, List<String> filters) {
    GraphQL graphQL = new GraphqlApiFactory().createGraphqlForSchema(table.getSchema());

    String graphQlQuery =
        new QueryBuilder(table).addAllColumns(MAX_QUERY_DEPTH).addFilters(filters).getQuery();
    ExecutionResult result = graphQL.execute(graphQlQuery);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode results = mapper.valueToTree(result.getData());
    JsonNode entryTypeResult = results.get(entryType.getId());
    if (entryTypeResult == null || entryTypeResult.isNull()) return null;

    return (ArrayNode) entryTypeResult;
  }

  private int doCountQuery(Table table, List<String> filters) {
    GraphQL graphQL = new GraphqlApiFactory().createGraphqlForSchema(table.getSchema());
    String graphQlQuery = new QueryBuilder(table).addFilters(filters).getCountQuery();

    ExecutionResult result = graphQL.execute(graphQlQuery);
    JsonNode results = mapper.valueToTree(result.getData());

    return results.get(entryType.getId() + "_agg").get("count").intValue();
  }

  private ArrayNode paginateResults(ArrayNode results) {
    if (results == null || results.isNull()) return null;

    int skip = beaconQuery.getPagination().getSkip();
    int limit = beaconQuery.getPagination().getLimit();

    if (limit == 0) limit = results.size();

    ArrayNode paginatedResults = JsonNodeFactory.instance.arrayNode();
    for (int i = skip; i < Math.min(skip + limit, results.size()); i++) {
      paginatedResults.add(results.get(i));
    }
    return paginatedResults;
  }

  private boolean isAuthorized(Table table) {
    List<String> roles = table.getSchema().getInheritedRolesForActiveUser();
    switch (this.granularity) {
      case BOOLEAN, COUNT, AGGREGATED:
        if (roles.contains(AGGREGATOR.toString())) return true;
      case RECORD, UNDEFINED:
        if (roles.contains(VIEWER.toString())) return true;
      default:
        return false;
    }
  }
}
