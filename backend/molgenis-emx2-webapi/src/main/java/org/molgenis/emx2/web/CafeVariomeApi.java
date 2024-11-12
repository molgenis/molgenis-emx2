package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.cafevariome.CafeVariomeQuery;
import org.molgenis.emx2.cafevariome.QueryRecord;

public class CafeVariomeApi {

  public static void create(Javalin app) {
    app.post("/{schema}/api/cafevariome/record", CafeVariomeApi::postRecord);
  }

  private static void postRecord(Context ctx) throws JsonProcessingException {

    ObjectMapper mapper = new ObjectMapper();
    CafeVariomeQuery query = mapper.readValue(ctx.body(), CafeVariomeQuery.class);

    Schema schema = getSchema(ctx);

    Object result = QueryRecord.post(schema, query);

    ctx.json(result);
  }
}
