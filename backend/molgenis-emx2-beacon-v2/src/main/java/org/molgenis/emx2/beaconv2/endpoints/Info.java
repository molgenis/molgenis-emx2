package org.molgenis.emx2.beaconv2.endpoints;

import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Parser;
import java.util.List;
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

    List<Row> infoRows =
        database
            .getSchema(schema.getName())
            .getTable(ENDPOINT_TABLE)
            .where(f("id", EQUALS, TABLE_ID))
            .retrieveRows();

    if (infoRows.isEmpty()) return null;

    Row infoRow = infoRows.getFirst();

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return mapper.valueToTree(infoRow.getValueMap());
  }
}
