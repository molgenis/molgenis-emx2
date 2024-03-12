package org.molgenis.emx2.beaconv2;

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
import java.util.List;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.beaconv2.requests.Filter;
import org.molgenis.emx2.graphql.GraphqlApiFactory;

public class QueryEntryType {

  public static JsonNode query(Database database, EntryType entryType, Filter... filters)
      throws JsltException {

    ObjectMapper mapper = new ObjectMapper();
    Expression jslt = Parser.compileResource(entryType.getName().toLowerCase() + ".jslt");

    ArrayNode resultSets = mapper.createArrayNode();
    for (Table table : getTableFromAllSchemas(database, entryType.getId())) {
      GraphQL graphQL = new GraphqlApiFactory().createGraphqlForSchema(table.getSchema());
      String query = new QueryBuilder(table).addColumns(2).addFilters(filters).getQuery();

      ExecutionResult result = graphQL.execute(query);
      JsonNode results = mapper.valueToTree(result.getData()).get(entryType.getId());

      ObjectNode resultSet = mapper.createObjectNode();
      resultSet.put("id", table.getSchema().getName());
      resultSet.set("results", results);
      resultSets.add(resultSet);
    }

    ObjectNode response = mapper.createObjectNode();
    response.set("resultSets", resultSets);

    return jslt.apply(response);
  }

  static List<Table> getTableFromAllSchemas(Database database, String tableName) {
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
