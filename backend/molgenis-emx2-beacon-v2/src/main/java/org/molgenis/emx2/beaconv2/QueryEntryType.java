package org.molgenis.emx2.beaconv2;

import static org.molgenis.emx2.Constants.SYSTEM_SCHEMA;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.JsltException;
import com.schibsted.spt.data.jslt.Parser;
import graphql.ExecutionResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.molgenis.emx2.*;
import org.molgenis.emx2.beaconv2.common.misc.Granularity;
import org.molgenis.emx2.beaconv2.common.misc.IncludedResultsetResponses;
import org.molgenis.emx2.beaconv2.filter.FilterParser;
import org.molgenis.emx2.beaconv2.filter.FilterParserFactory;
import org.molgenis.emx2.beaconv2.requests.BeaconQuery;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;
import org.molgenis.emx2.graphql.GraphqlExecutor;

public class QueryEntryType {

  private static final int MAX_QUERY_DEPTH = 3;

  private final BeaconRequestBody request;
  private final BeaconQuery beaconQuery;
  private final EntryType entryType;
  private final Granularity granularity;
  private final IncludedResultsetResponses includeStrategy;

  private static final ObjectMapper mapper = new ObjectMapper();

  public QueryEntryType(BeaconRequestBody request) {
    this.request = request;
    this.beaconQuery = request.getQuery();
    this.entryType = request.getQuery().getEntryType();
    this.granularity = request.getQuery().getRequestedGranularity();
    this.includeStrategy = request.getQuery().getIncludeResultsetResponses();
  }

  public JsonNode query(Schema schema) {
    Database database = schema.getDatabase();
    ResolvedTemplate template = resolveTemplates(database).forSchema(schema.getName());
    ObjectNode response = mapper.createObjectNode();
    response.set("requestBody", mapper.valueToTree(request));
    FilterParser filterParser = parseFilters(response);

    int numTotalResults = 0;
    ArrayNode resultSets = mapper.createArrayNode();
    Table table = schema.getTable(template.tableId());
    if (table == null) {
      throw new MolgenisException("Table " + template.tableId() + " does not exist");
    }
    numTotalResults = queryTable(table, filterParser, resultSets);

    if (!granularity.equals(Granularity.BOOLEAN)) {
      response.put("numTotalResults", numTotalResults);
    }
    response.set("resultSets", resultSets);
    return getJsltResponse(template.jslt(), response);
  }

  public JsonNode query(Database database) throws JsltException {
    ObjectNode response = mapper.createObjectNode();
    response.set("requestBody", mapper.valueToTree(request));
    FilterParser filterParser = parseFilters(response);
    Templates templates = resolveTemplates(database);

    int numTotalResults = 0;
    ArrayNode resultSets = mapper.createArrayNode();
    ArrayNode renderedEntries = mapper.createArrayNode();

    for (String schemaName : database.getSchemaNames()) {
      Schema entrySchema = database.getSchema(schemaName);
      if (entrySchema == null) continue;
      ResolvedTemplate template = templates.forSchema(schemaName);
      Table table = entrySchema.getTable(template.tableId());
      if (table == null) continue;

      ArrayNode schemaResultSets = mapper.createArrayNode();
      int schemaResults = queryTable(table, filterParser, schemaResultSets);
      if (schemaResultSets.isEmpty()) continue;

      numTotalResults += schemaResults;
      resultSets.addAll(schemaResultSets);
      renderedEntries.addAll(renderEntries(template, response, schemaResultSets, schemaResults));
    }
    if (!granularity.equals(Granularity.BOOLEAN)) {
      response.put("numTotalResults", numTotalResults);
    }
    response.set("resultSets", resultSets);

    ObjectNode jsltResponse = getJsltResponse(null, response);
    setEntries(jsltResponse, renderedEntries);
    return jsltResponse;
  }

  private ArrayNode renderEntries(
      ResolvedTemplate template, ObjectNode response, ArrayNode schemaResultSets, int count) {
    ObjectNode schemaResponse = response.deepCopy();
    if (!granularity.equals(Granularity.BOOLEAN)) {
      schemaResponse.put("numTotalResults", count);
    }
    schemaResponse.set("resultSets", schemaResultSets);

    JsonNode entries =
        getJsltResponse(template.jslt(), schemaResponse).path("response").path(entriesField());
    if (!entries.isArray()) {
      throw new MolgenisException(
          "Beacon template for endpoint beacon_"
              + entryType.getName()
              + " does not produce response."
              + entriesField()
              + ", so it cannot be combined with other schemas in a cross-schema query");
    }
    return (ArrayNode) entries;
  }

  private void setEntries(ObjectNode jsltResponse, ArrayNode entries) {
    if (jsltResponse.get("response") instanceof ObjectNode responseNode) {
      responseNode.set(entriesField(), entries);
    }
  }

  private String entriesField() {
    return switch (entryType) {
      case COHORTS, DATASETS -> "collections";
      default -> "resultSets";
    };
  }

  private int queryTable(Table table, FilterParser filterParser, ArrayNode resultSets) {
    if (!hasPermissionForGranularity(table.getSchema(), table.getMetadata())) return 0;
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

  private ObjectNode getJsltResponse(String jsltOverride, ObjectNode response) {
    ArrayNode resultSets = response.withArray("resultSets");

    Expression jslt;
    if (jsltOverride != null) {
      jslt = Parser.compileString(jsltOverride);
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

  private record ResolvedTemplate(String tableId, String jslt) {}

  private record Templates(Map<String, ResolvedTemplate> bySchema, ResolvedTemplate fallback) {
    ResolvedTemplate forSchema(String schemaName) {
      return bySchema.getOrDefault(schemaName, fallback);
    }
  }

  private Templates resolveTemplates(Database database) {
    Map<String, ResolvedTemplate> bySchema = new HashMap<>();
    String endpoint = "beacon_" + entryType.getName();
    database.tx(
        tx -> {
          String activeUser = tx.getActiveUser();
          try {
            tx.becomeAdmin();
            Table templatesTable = tx.getSchema(SYSTEM_SCHEMA).getTable("Templates");
            templatesTable.retrieveRows().stream()
                .filter(r -> endpoint.equals(r.getString("endpoint")))
                .forEach(r -> bySchema.putIfAbsent(r.getString("schema"), toResolvedTemplate(r)));
          } finally {
            tx.setActiveUser(activeUser);
          }
        });
    return new Templates(bySchema, new ResolvedTemplate(entryType.getId(), null));
  }

  private ResolvedTemplate toResolvedTemplate(Row row) {
    String tableId = Objects.requireNonNullElse(row.getString("tableName"), entryType.getId());
    return new ResolvedTemplate(tableId, row.getString("template"));
  }

  private void addEmptyResultSet(ObjectNode jsltResponse) {
    jsltResponse.set(
        "response", mapper.createObjectNode().set(entriesField(), mapper.createArrayNode()));
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
    GraphqlExecutor graphQL = new GraphqlExecutor(table.getSchema());

    String graphQlQuery =
        new QueryBuilder(table)
            .addAllColumns(MAX_QUERY_DEPTH)
            .setLimit(beaconQuery.getPagination().getLimit())
            .setOffset(beaconQuery.getPagination().getSkip())
            .addFilters(filters)
            .getQuery();
    ExecutionResult result = graphQL.executeWithoutSession(graphQlQuery);

    JsonNode results = mapper.valueToTree(result.getData());
    JsonNode entryTypeResult = results.get(table.getIdentifier());
    if (entryTypeResult == null || entryTypeResult.isNull()) return null;

    return (ArrayNode) entryTypeResult;
  }

  public static int doCountQuery(Table table, List<String> filters) {
    GraphqlExecutor graphQL = new GraphqlExecutor(table.getSchema());
    String graphQlQuery = new QueryBuilder(table).addFilters(filters).getCountQuery();

    ExecutionResult result = graphQL.executeWithoutSession(graphQlQuery);
    JsonNode results = mapper.valueToTree(result.getData());

    return results.get(table.getIdentifier() + "_agg").get("count").intValue();
  }

  public static boolean doExistsQuery(Table table, List<String> filters) {
    GraphqlExecutor graphQL = new GraphqlExecutor(table.getSchema());
    String graphQlQuery = new QueryBuilder(table).addFilters(filters).getExistsQuery();

    ExecutionResult result = graphQL.executeWithoutSession(graphQlQuery);
    JsonNode results = mapper.valueToTree(result.getData());

    return results.get(table.getIdentifier() + "_agg").get("exists").booleanValue();
  }

  private boolean hasPermissionForGranularity(Schema schema, TableMetadata table) {
    return switch (this.granularity) {
      case BOOLEAN -> PermissionEvaluator.canExists(schema, table);
      case COUNT, AGGREGATED -> PermissionEvaluator.canRange(schema, table);
      case RECORD, UNDEFINED -> PermissionEvaluator.canView(schema, table);
    };
  }
}
