package org.molgenis.emx2.web;

import static org.molgenis.emx2.json.JsonUtil.*;
import static org.molgenis.emx2.web.Constants.*;
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
    // schema level operations
    final String jsonPath = "/{schema}/api/json";
    app.get(jsonPath, JsonYamlApi::getSchemaJSON);
    app.post(jsonPath, JsonYamlApi::postSchemaJSON);
    app.delete(jsonPath, JsonYamlApi::deleteSchemaJSON);

    final String yamlPath = "/{schema}/api/yaml";
    app.get(yamlPath, JsonYamlApi::getSchemaYAML);
    app.post(yamlPath, JsonYamlApi::postSchemaYAML);
    app.delete(yamlPath, JsonYamlApi::deleteSchemaYAML);
  }

  private static void deleteSchemaYAML(Context ctx) throws IOException {
    SchemaMetadata schema = yamlToSchema(ctx.body());
    getSchema(ctx).discard(schema);
    ctx.status(200);
    ctx.result("removed metadata items success");
  }

  static void postSchemaYAML(Context ctx) throws IOException {
    SchemaMetadata otherSchema = yamlToSchema(ctx.body());
    getSchema(ctx).migrate(otherSchema);
    ctx.status(200);
    ctx.result("patch metadata success");
  }

  static void getSchemaYAML(Context ctx) throws IOException {
    Schema schema = getSchema(ctx);
    String json = schemaToYaml(schema.getMetadata(), true);
    ctx.contentType(ACCEPT_YAML);
    String date = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
    ctx.header(
        "Content-Disposition",
        "attachment; filename=\"" + schema.getName() + "_ " + date + ".yaml\"");
    ctx.status(200);
    ctx.json(json);
  }

  private static void deleteSchemaJSON(Context ctx) throws IOException {
    SchemaMetadata schema = jsonToSchema(ctx.body());
    getSchema(ctx).discard(schema);
    ctx.status(200);
    ctx.result("removed metadata items success");
  }

  static void postSchemaJSON(Context ctx) throws IOException {
    Schema schema = getSchema(ctx);
    SchemaMetadata otherSchema = jsonToSchema(ctx.body());
    schema.migrate(otherSchema);
    ctx.status(200);
    ctx.result("patch metadata success");
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
