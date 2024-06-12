package org.molgenis.emx2.beaconv2.endpoints;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Parser;
import graphql.ExecutionResult;
import graphql.GraphQL;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.beaconv2.QueryBuilder;
import org.molgenis.emx2.graphql.GraphqlApiFactory;

public class Info {

  public static final String ENDPOINT_TABLE = "Endpoint";
  public static final String JSLT_PATH = "informational/info.jslt";

  private final Schema schema;

  public Info(Schema schema) {
    this.schema = schema;
  }

  public JsonNode getResponse() {
    JsonNode info = getEndpointInfo(schema);
    Expression jslt = Parser.compileResource(JSLT_PATH);
    return jslt.apply(info);
  }

  private JsonNode getEndpointInfo(Schema schema) {
    if (schema == null) return null; // todo: get global info or schema only?
    if (schema.getTable(ENDPOINT_TABLE) == null) return null;

    Table table = schema.getTable(ENDPOINT_TABLE);

    QueryBuilder queryBuilder = new QueryBuilder(table).addAllColumns(2);
    GraphQL graphQL = new GraphqlApiFactory().createGraphqlForSchema(schema);
    ExecutionResult result = graphQL.execute(queryBuilder.getQuery());

    ObjectMapper mapper = new ObjectMapper();
    return mapper.valueToTree(result.getData()).get(ENDPOINT_TABLE).get(0);
  }
}
