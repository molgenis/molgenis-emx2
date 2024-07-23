package org.molgenis.emx2.beaconv2.endpoints;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Parser;
import java.util.List;
import org.molgenis.emx2.*;

public class Info {

  public static final String ENDPOINT_TABLE = "Endpoint";
  public static final String JSLT_PATH = "informational/info.jslt";

  private final Database database;

  public Info(Database database) {
    this.database = database;
  }

  public JsonNode getResponse(Schema schema) {
    JsonNode info = getEndpointInfo(schema);
    Expression jslt = Parser.compileResource(JSLT_PATH);
    return jslt.apply(info);
  }

  private JsonNode getEndpointInfo(Schema schema) {
    if (schema == null) {
      throw new MolgenisException("Informational endpoint is only available on schema level");
    }
    if (schema.getTable(ENDPOINT_TABLE) == null) return null;

    String query = "SELECT * FROM \"" + schema.getName() + "\".\"" + ENDPOINT_TABLE + "\"";
    List<Row> rows = database.getSchema(schema.getName()).retrieveSql(query);

    ObjectMapper mapper = new ObjectMapper();
    return mapper.valueToTree(rows.get(0).getValueMap());
  }
}
