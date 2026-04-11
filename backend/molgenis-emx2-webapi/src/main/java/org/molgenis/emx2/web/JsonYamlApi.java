package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.Constants.*;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;

import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.emx2.Emx2Yaml;
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
    SchemaMetadata schema =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(ctx.body().getBytes(StandardCharsets.UTF_8)))
            .getSchema();
    getSchema(ctx).discard(schema);
    ctx.status(200);
    ctx.result("{ \"message\": \"remove metadata success\" }");
  }

  static void postSchemaYAML(Context ctx) throws IOException {
    SchemaMetadata otherSchema =
        Emx2Yaml.fromBundle(new ByteArrayInputStream(ctx.body().getBytes(StandardCharsets.UTF_8)))
            .getSchema();
    getSchema(ctx).migrate(otherSchema);
    ctx.status(200);
    ctx.result("{ \"message\": \"add/update metadata success\" }");
  }

  static void getSchemaYAML(Context ctx) throws IOException {
    Schema schema = getSchema(ctx);
    Path tempFile = Files.createTempFile("yaml_export_", ".yaml");
    try {
      Emx2Yaml.toBundleSingleFile(schema.getMetadata(), schema.getName(), null, tempFile, List.of());
      ctx.contentType(ACCEPT_YAML);
      String date = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
      ctx.header(
          "Content-Disposition",
          "attachment; filename=\"" + schema.getName() + "_" + date + ".yaml\"");
      ctx.status(200);
      ctx.result(Files.readString(tempFile));
    } finally {
      Files.deleteIfExists(tempFile);
    }
  }

  private static void deleteSchemaJSON(Context ctx) throws IOException {
    SchemaMetadata schema = JsonUtil.jsonToSchema(ctx.body());
    getSchema(ctx).discard(schema);
    ctx.status(200);
    ctx.result("{ \"message\": \"removed metadata items success\" }");
  }

  static void postSchemaJSON(Context ctx) throws IOException {
    Schema schema = getSchema(ctx);
    SchemaMetadata otherSchema = JsonUtil.jsonToSchema(ctx.body());
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
