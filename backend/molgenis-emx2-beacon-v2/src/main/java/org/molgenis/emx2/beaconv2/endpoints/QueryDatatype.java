package org.molgenis.emx2.beaconv2.endpoints;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Parser;
import graphql.ExecutionResult;
import java.util.List;
import org.molgenis.emx2.Table;

public class QueryDatatype {

  /**
   * Construct GraphQL query on Beacon v2 individuals, with optional filters like "{sex:
   * {ontologyTermURI: {like: "http://purl.obolibrary.org/obo/GSSO_000124"}}}", "{ diseases: {
   * diseaseCode: {ontologyTermURI: {like: "Orphanet_1873"}}}}", etc. Beacon supports only AND
   * operator for filters.
   *
   * @param tables
   * @param filters
   * @return
   */
  public static JsonNode query(List<Table> tables, String tableId, String... filters) {

    ObjectMapper mapper = new ObjectMapper();
    Expression jslt = Parser.compileResource(tableId.toLowerCase() + ".jslt");

    ArrayNode resultSets = mapper.createArrayNode();
    for (Table table : tables) {
      ExecutionResult executionResult = QueryHelper.queryTable(table);
      JsonNode results = mapper.valueToTree(executionResult.getData()).get(tableId);

      ObjectNode resultSet = mapper.createObjectNode();
      resultSet.put("id", table.getSchema().getName());
      resultSet.set("results", results);
      resultSets.add(resultSet);
    }

    ObjectNode response = mapper.createObjectNode();
    response.set("resultSets", resultSets);

    return jslt.apply(response);
  }
}
