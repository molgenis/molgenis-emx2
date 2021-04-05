package org.molgenis.emx2.web;

import static org.molgenis.emx2.json.JsonUtil.*;
import static org.molgenis.emx2.web.Constants.*;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static spark.Spark.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.molgenis.emx2.*;
import org.molgenis.emx2.json.JsonUtil;
import spark.Request;
import spark.Response;

public class JsonYamlApi {

  public static final String MUTATION_REQUEST = "mutationRequestType";
  public static final String ERROR_MESSAGE = "errorMessageType";
  public static final String SUCCESS_MESSAGE = "successMessageType";
  public static final String CSV_OUTPUT = "csvOutputType";
  public static final String META = "_meta";

  private JsonYamlApi() {
    // hide constructor
  }

  public static void create() {

    // schema level operations
    final String jsonPath = "/:schema/api/json";
    get(jsonPath, JsonYamlApi::getSchemaJSON);
    post(jsonPath, JsonYamlApi::postSchemaJSON);
    delete(jsonPath, JsonYamlApi::deleteSchemaJSON);

    final String yamlPath = "/:schema/api/yaml";
    get(yamlPath, JsonYamlApi::getSchemaYAML);
    post(yamlPath, JsonYamlApi::postSchemaYAML);
    delete(yamlPath, JsonYamlApi::deleteSchemaYAML);
  }

  private static String deleteSchemaYAML(Request request, Response response) throws IOException {
    SchemaMetadata schema = yamlToSchema(request.body());
    getSchema(request).discard(schema);
    response.status(200);
    return "removed metadata items success";
  }

  static String postSchemaYAML(Request request, Response response) throws IOException {
    SchemaMetadata otherSchema = yamlToSchema(request.body());
    getSchema(request).migrate(otherSchema);
    response.status(200);
    return "patch metadata success";
  }

  static String getSchemaYAML(Request request, Response response) throws IOException {
    Schema schema = getSchema(request);
    String json = schemaToYaml(schema.getMetadata(), true);
    response.type(ACCEPT_YAML);
    String date = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
    response.header(
        "Content-Disposition",
        "attachment; filename=\"" + schema.getName() + "_ " + date + ".yaml\"");
    response.status(200);
    return json;
  }

  private static String deleteSchemaJSON(Request request, Response response) throws IOException {
    SchemaMetadata schema = jsonToSchema(request.body());
    getSchema(request).discard(schema);
    response.status(200);
    return "removed metadata items success";
  }

  static String postSchemaJSON(Request request, Response response) throws IOException {
    Schema schema = getSchema(request);
    SchemaMetadata otherSchema = jsonToSchema(request.body());
    schema.migrate(otherSchema);
    response.status(200);
    return "patch metadata success";
  }

  static String getSchemaJSON(Request request, Response response) throws IOException {
    Schema schema = getSchema(request);
    String json = JsonUtil.schemaToJson(schema.getMetadata(), true);
    response.status(200);
    response.type(ACCEPT_YAML);
    String date = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
    response.header(
        "Content-Disposition",
        "attachment; filename=\"" + schema.getName() + "_ " + date + ".json\"");
    return json;
  }
}
