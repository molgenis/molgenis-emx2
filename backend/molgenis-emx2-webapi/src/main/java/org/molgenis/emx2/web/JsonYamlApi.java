package org.molgenis.emx2.web;

import static org.molgenis.emx2.json.JsonUtil.*;
import static org.molgenis.emx2.web.Constants.*;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.molgenis.emx2.*;
import org.molgenis.emx2.json.JsonUtil;

public class JsonYamlApi {

  private static final ObjectMapper MAPPER = new ObjectMapper();

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
    Schema schema = getSchema(ctx);
    if (schema == null || !PermissionEvaluator.canManage(schema)) {
      throw new MolgenisException("Schema not found or insufficient access");
    }

    SchemaMetadata otherSchema = yamlToSchema(ctx.body());
    schema.discard(otherSchema);

    HashMap<String, String> response = new HashMap<>();
    response.put("message", "remove metadata success");
    if (!Objects.equals(otherSchema.getName(), schema.getName())) {
      response.put("warning", "schema name mismatch");
    }

    ctx.status(200);
    ctx.result(MAPPER.writeValueAsString(response));
  }

  static void postSchemaYAML(Context ctx) throws IOException {
    Schema schema = getSchema(ctx);
    if (schema == null || !PermissionEvaluator.canManage(schema)) {
      throw new MolgenisException("Schema not found or insufficient access");
    }

    SchemaMetadata otherSchema = yamlToSchema(ctx.body());
    schema.migrate(otherSchema);

    HashMap<String, String> response = new HashMap<>();
    response.put("message", "add/update metadata success");
    if (!Objects.equals(otherSchema.getName(), schema.getName())) {
      response.put("warning", "schema name mismatch");
    }

    ctx.status(200);
    ctx.result(MAPPER.writeValueAsString(response));
  }

  static void getSchemaYAML(Context ctx) throws IOException {
    Schema schema = getSchema(ctx);
    if (schema == null) {
      throw new MolgenisException("Schema not found or insufficient access");
    }

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
    Schema schema = getSchema(ctx);
    if (schema == null || !PermissionEvaluator.canManage(schema)) {
      throw new MolgenisException("Schema not found or insufficient access");
    }

    SchemaMetadata otherSchema = jsonToSchema(ctx.body());
    schema.discard(otherSchema);

    HashMap<String, String> response = new HashMap<>();
    response.put("message", "removed metadata items success");
    if (!Objects.equals(otherSchema.getName(), schema.getName())) {
      response.put("warning", "schema name mismatch");
    }

    ctx.status(200);
    ctx.result(MAPPER.writeValueAsString(response));
  }

  static void postSchemaJSON(Context ctx) throws IOException {
    Schema schema = getSchema(ctx);
    if (schema == null || !PermissionEvaluator.canManage(schema)) {
      throw new MolgenisException("Schema not found or insufficient access");
    }

    SchemaMetadata otherSchema = jsonToSchema(ctx.body());
    schema.migrate(otherSchema);

    Map<String, String> response = new HashMap<>();
    response.put("message", "add/update metadata success");
    if (!Objects.equals(schema.getName(), otherSchema.getName())) {
      response.put("warning", "schema name mismatch");
    }

    ctx.status(200);
    ctx.result(MAPPER.writeValueAsString(response));
  }

  static void getSchemaJSON(Context ctx) throws IOException {
    Schema schema = getSchema(ctx);
    if (schema == null) {
      throw new MolgenisException("Schema not found or insufficient access");
    }

    String json = JsonUtil.schemaToJson(schema.getMetadata(), true);
    ctx.status(200);
    String date = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
    ctx.header(
        "Content-Disposition",
        "attachment; filename=\"" + schema.getName() + "_ " + date + ".json\"");
    ctx.json(json);
  }
}
