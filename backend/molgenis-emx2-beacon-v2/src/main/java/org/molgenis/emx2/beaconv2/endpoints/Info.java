package org.molgenis.emx2.beaconv2.endpoints;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Parser;
import org.molgenis.emx2.*;

public class Info {

  private static final String ENDPOINT_TABLE = "Endpoint";
  private static final String JSLT_PATH = "informational/info.jslt";
  private static final String TABLE_ID = "org.molgeniscloud.beaconv2";

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

    String query =
        """
            SELECT * FROM "%1$s"."%2$s" WHERE id = '%3$s'
            """
            .formatted(schema.getName(), ENDPOINT_TABLE, TABLE_ID);

    Row row = database.getSchema(schema.getName()).retrieveSql(query).get(0);

    ObjectMapper mapper = new ObjectMapper();
    return mapper.valueToTree(row.getValueMap());
  }
}
