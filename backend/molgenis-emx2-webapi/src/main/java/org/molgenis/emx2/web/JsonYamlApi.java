package org.molgenis.emx2.web;

import static org.molgenis.emx2.json.JsonUtil.*;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;

import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.molgenis.emx2.*;
import org.molgenis.emx2.json.JsonUtil;

public class JsonYamlApi {

  private JsonYamlApi() {
    // hide constructor
  }

  public static void create(Javalin app) {
    // schema level operations; the /api/yaml route is served by ModelApi (bundle single-file form)
    final String jsonPath = "/{schema}/api/json";
    app.get(jsonPath, JsonYamlApi::getSchemaJSON);
    app.post(jsonPath, JsonYamlApi::postSchemaJSON);
    app.delete(jsonPath, JsonYamlApi::deleteSchemaJSON);
  }

  private static void deleteSchemaJSON(Context ctx) throws IOException {
    SchemaMetadata schema = jsonToSchema(ctx.body());
    getSchema(ctx).discard(schema);
    ctx.status(200);
    ctx.result("{ \"message\": \"removed metadata items success\" }");
  }

  static void postSchemaJSON(Context ctx) throws IOException {
    Schema schema = getSchema(ctx);
    SchemaMetadata otherSchema = jsonToSchema(ctx.body());
    schema.migrate(otherSchema);
    ctx.status(200);
    ctx.result("{ \"message\": \"add/update metadata success\" }");
  }

  static void getSchemaJSON(Context ctx) throws IOException {
    Schema schema = getSchema(ctx);
    String json = JsonUtil.schemaToJson(schema.getMetadata(), true);
    ctx.status(200);
    String date = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
    ctx.header(
        "Content-Disposition",
        "attachment; filename=\"" + schema.getName() + "_ " + date + ".json\"");
    ctx.json(json);
  }
}
