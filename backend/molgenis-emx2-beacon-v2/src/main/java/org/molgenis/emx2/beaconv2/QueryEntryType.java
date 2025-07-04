package org.molgenis.emx2.beaconv2;

import static org.molgenis.emx2.Constants.SYSTEM_SCHEMA;
import static org.molgenis.emx2.Privileges.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.JsltException;
import com.schibsted.spt.data.jslt.Parser;
import graphql.ExecutionResult;
import graphql.GraphQL;
import java.util.List;
import org.molgenis.emx2.*;
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
  private Database database;
  private Schema schema;

  public QueryEntryType(BeaconRequestBody request) {
    this.request = request;
    this.beaconQuery = request.getQuery();
    this.entryType = request.getQuery().getEntryType();
    this.granularity = request.getQuery().getRequestedGranularity();
    this.includeStrategy = request.getQuery().getIncludeResultsetResponses();
  }

  public JsonNode query(Schema schema) {
    if (schema != null) {
      this.database = schema.getDatabase();
      this.schema = schema;
    }
    ObjectNode response = mapper.createObjectNode();
    response.set("requestBody", mapper.valueToTree(request));
    FilterParser filterParser = parseFilters(response);

    int numTotalResults = 0;
    ArrayNode resultSets = mapper.createArrayNode();
    if (schema != null && isAuthorized(schema.getInheritedRolesForActiveUser())) {
      Table table = schema.getTable(entryType.getId());
      numTotalResults = queryTable(table, filterParser, resultSets);
    }

    if (!granularity.equals(Granularity.BOOLEAN)) {
      response.put("numTotalResults", numTotalResults);
    }
    response.set("resultSets", resultSets);
    return getJsltResponse(response);
  }

  public JsonNode query(Database database) throws JsltException {
    this.database = database;
    ObjectNode response = mapper.createObjectNode();
    response.set("requestBody", mapper.valueToTree(request));
    FilterParser filterParser = parseFilters(response);

    int numTotalResults = 0;
    ArrayNode resultSets = mapper.createArrayNode();

    List<Table> entryTypeTables = database.getTablesFromAllSchemas(entryType.getId());
    for (Table table : entryTypeTables) {
      numTotalResults += queryTable(table, filterParser, resultSets);
    }
    if (!granularity.equals(Granularity.BOOLEAN)) {
      response.put("numTotalResults", numTotalResults);
    }
    response.set("resultSets", resultSets);
    return getJsltResponse(response);
  }

  private int queryTable(Table table, FilterParser filterParser, ArrayNode resultSets) {
    if (!isAuthorized(table.getSchema().getInheritedRolesForActiveUser())) return 0;
    int numTotalResults = 0;
    ObjectNode resultSet = mapper.createObjectNode();
    resultSet.put("id", table.getSchema().getName());
    resultSet.put("role", table.getSchema().getRoleForActiveUser());

    switch (granularity) {
      case RECORD, UNDEFINED:
        ArrayNode resultsArray = doGraphQlQuery(table, filterParser.getGraphQlFilters());
        if (hasResult(resultsArray)) {
          int count = doCountQuery(table, filterParser.getGraphQlFilters());
          numTotalResults += count;
          resultSet.set("results", resultsArray);
          resultSet.put("count", count);
          resultSets.add(resultSet);
        } else if (includeStrategy.equals(IncludedResultsetResponses.ALL)) {
          resultSets.add(resultSet);
        }
        break;
      case COUNT, AGGREGATED:
        int count = doCountQuery(table, filterParser.getGraphQlFilters());
        if (count > 0) {
          resultSet.put("exist", true);
          numTotalResults += count;
          resultSet.put("count", count);
          resultSets.add(resultSet);
        } else if (includeStrategy.equals(IncludedResultsetResponses.ALL)) {
          resultSets.add(resultSet);
        }
        break;
      case BOOLEAN:
        boolean exists = doExistsQuery(table, filterParser.getGraphQlFilters());
        if (exists) {
          resultSet.put("exist", true);
          resultSets.add(resultSet);
        } else if (includeStrategy.equals(IncludedResultsetResponses.ALL)) {
          resultSets.add(resultSet);
        }
        break;
    }

    return numTotalResults;
  }

  private ObjectNode getJsltResponse(ObjectNode response) {
    ArrayNode resultSets = response.withArray("resultSets");

    String template = null;
    if (database != null && schema != null) {
      database.becomeAdmin();
      Schema systemSchema = database.getSchema(SYSTEM_SCHEMA);
      Table templatesTable = systemSchema.getTable("Templates");
      List<Row> templates = templatesTable.retrieveRows();
      template =
          templates.stream()
              .filter(
                  r ->
                      r.getString("schema").equals(schema.getName())
                          && r.getString("endpoint").equals("beacon_" + entryType.getName()))
              .map(r -> r.get("template", String.class))
              .findFirst()
              .orElse(null);
    }

    Expression jslt;
    if (template != null) {
      jslt = Parser.compileString(template);
    } else {
      String jsltPath = "entry-types/" + entryType.getName().toLowerCase() + ".jslt";
      jslt = Parser.compileResource(jsltPath);
    }

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
    GraphQL graphQL = new GraphqlApiFactory().createGraphqlForSchema(table.getSchema(), null);

    String graphQlQuery =
        new QueryBuilder(table)
            .addAllColumns(MAX_QUERY_DEPTH)
            .setLimit(beaconQuery.getPagination().getLimit())
            .setOffset(beaconQuery.getPagination().getSkip())
            .addFilters(filters)
            .getQuery();
    ExecutionResult result = graphQL.execute(graphQlQuery);

    JsonNode results = mapper.valueToTree(result.getData());
    JsonNode entryTypeResult = results.get(entryType.getId());
    if (entryTypeResult == null || entryTypeResult.isNull()) return null;

    return (ArrayNode) entryTypeResult;
  }

  private int doCountQuery(Table table, List<String> filters) {
    GraphQL graphQL = new GraphqlApiFactory().createGraphqlForSchema(table.getSchema(), null);
    String graphQlQuery = new QueryBuilder(table).addFilters(filters).getCountQuery();

    ExecutionResult result = graphQL.execute(graphQlQuery);
    JsonNode results = mapper.valueToTree(result.getData());

    return results.get(entryType.getId() + "_agg").get("count").intValue();
  }

  private boolean doExistsQuery(Table table, List<String> filters) {
    GraphQL graphQL = new GraphqlApiFactory().createGraphqlForSchema(table.getSchema(), null);
    String graphQlQuery = new QueryBuilder(table).addFilters(filters).getExistsQuery();

    ExecutionResult result = graphQL.execute(graphQlQuery);
    JsonNode results = mapper.valueToTree(result.getData());

    return results.get(entryType.getId() + "_agg").get("exists").booleanValue();
  }

  private boolean isAuthorized(List<String> roles) {
    switch (this.granularity) {
      case BOOLEAN:
        if (roles.contains(EXISTS.toString())) return true;
      case COUNT, AGGREGATED:
        if (roles.contains(RANGE.toString())) return true;
      case RECORD, UNDEFINED:
        if (roles.contains(VIEWER.toString())) return true;
      default:
        return false;
    }
  }
}
