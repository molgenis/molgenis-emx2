package org.molgenis.emx2.beaconv2.endpoints.individuals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Parser;
import graphql.ExecutionResult;
import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.beaconv2.endpoints.QueryHelper;

public class QueryIndividuals {

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
  public static List<IndividualsResultSets> queryIndividuals(List<Table> tables, String... filters)
      throws JsonProcessingException {

    ObjectMapper mapper = new ObjectMapper();
    Expression jslt = Parser.compileResource("individuals.jslt");

    List<IndividualsResultSets> resultSetsList = new ArrayList<>();
    for (Table table : tables) {
      ExecutionResult result = QueryHelper.queryTable(table);
      JsonNode individuals = mapper.valueToTree(result.getData()).get("Individuals");

      ArrayNode individualsOutput = mapper.createArrayNode();
      for (JsonNode individual : individuals) individualsOutput.add(jslt.apply(individual));

      if (!individualsOutput.isEmpty()) {
        IndividualsResultSets individualsResultSets =
            new IndividualsResultSets(table.getSchema().getName(), individualsOutput);
        resultSetsList.add(individualsResultSets);
      }
    }
    return resultSetsList;
  }
}
