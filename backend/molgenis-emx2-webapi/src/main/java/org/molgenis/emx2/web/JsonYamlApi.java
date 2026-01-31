package org.molgenis.emx2.web;

import static org.molgenis.emx2.json.JsonUtil.*;
import static org.molgenis.emx2.web.Constants.*;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.*;
import org.molgenis.emx2.json.JsonUtil;
import org.molgenis.emx2.utils.TypeUtils;

public class JsonYamlApi {

  private JsonYamlApi() {
    // hide constructor
  }

  public static void create(Javalin app) {
    final String jsonSchemaPath = "/{schema}/api/json/_schema";
    app.get(jsonSchemaPath, JsonYamlApi::getSchemaJSON);
    app.post(jsonSchemaPath, JsonYamlApi::postSchemaJSON);
    app.delete(jsonSchemaPath, JsonYamlApi::deleteSchemaJSON);

    final String yamlSchemaPath = "/{schema}/api/yaml/_schema";
    app.get(yamlSchemaPath, JsonYamlApi::getSchemaYAML);
    app.post(yamlSchemaPath, JsonYamlApi::postSchemaYAML);
    app.delete(yamlSchemaPath, JsonYamlApi::deleteSchemaYAML);

    final String jsonTablePath = "/{schema}/api/json/{table}";
    app.get(jsonTablePath, ctx -> tableRetrieveJson(ctx));
    app.post(jsonTablePath, ctx -> tableUpdate(ctx, new ObjectMapper()));
    app.delete(jsonTablePath, ctx -> tableDelete(ctx, new ObjectMapper()));

    final String yamlTablePath = "/{schema}/api/yaml/{table}";
    app.get(yamlTablePath, ctx -> tableRetrieveYaml(ctx));
    app.post(yamlTablePath, ctx -> tableUpdate(ctx, new ObjectMapper(new YAMLFactory())));
    app.delete(yamlTablePath, ctx -> tableDelete(ctx, new ObjectMapper(new YAMLFactory())));
  }

  private static void deleteSchemaYAML(Context ctx) throws IOException {
    SchemaMetadata schema = yamlToSchema(ctx.body());
    getSchema(ctx).discard(schema);
    ctx.status(200);
    ctx.result("{ \"message\": \"remove metadata success\" }");
  }

  static void postSchemaYAML(Context ctx) throws IOException {
    SchemaMetadata otherSchema = yamlToSchema(ctx.body());
    getSchema(ctx).migrate(otherSchema);
    ctx.status(200);
    ctx.result("{ \"message\": \"add/update metadata success\" }");
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

  private static void tableRetrieveJson(Context ctx) {
    Table table = MolgenisWebservice.getTableByIdOrName(ctx);
    String json = table.query().retrieveJSON();
    ctx.status(200);
    ctx.contentType(ACCEPT_JSON);
    ctx.header("Content-Disposition", "attachment; filename=\"" + table.getName() + ".json\"");
    ctx.result(json);
  }

  private static void tableRetrieveYaml(Context ctx) throws IOException {
    Table table = MolgenisWebservice.getTableByIdOrName(ctx);
    String json = table.query().retrieveJSON();
    ObjectMapper jsonMapper = new ObjectMapper();
    ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    Object data = jsonMapper.readValue(json, Object.class);
    String yaml = yamlMapper.writeValueAsString(data);
    ctx.status(200);
    ctx.contentType(ACCEPT_YAML);
    ctx.header("Content-Disposition", "attachment; filename=\"" + table.getName() + ".yaml\"");
    ctx.result(yaml);
  }

  private static void tableUpdate(Context ctx, ObjectMapper mapper) throws IOException {
    Table table = MolgenisWebservice.getTableByIdOrName(ctx);
    List<Map<String, Object>> rowMaps = mapper.readValue(ctx.body(), List.class);
    List<Row> rows = TypeUtils.convertToRows(table.getMetadata(), rowMaps);
    int count = table.save(rows);
    ctx.status(200);
    ctx.result("{ \"message\": \"imported number of rows: " + count + "\" }");
  }

  private static void tableDelete(Context ctx, ObjectMapper mapper) throws IOException {
    Table table = MolgenisWebservice.getTableByIdOrName(ctx);
    List<Map<String, Object>> rowMaps = mapper.readValue(ctx.body(), List.class);
    List<Row> rows = TypeUtils.convertToPrimaryKeyRows(table.getMetadata(), rowMaps);
    int count = table.delete(rows);
    ctx.status(200);
    ctx.result("{ \"message\": \"deleted number of rows: " + count + "\" }");
  }
}
