package org.molgenis.emx2.web;

import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.JsonException;
import io.swagger.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import org.molgenis.*;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.molgenis.Row.MOLGENISID;
import static org.molgenis.emx2.web.JsonRowMapper.rowToJson;
import static org.molgenis.emx2.web.OpenApiFactory.*;
import static org.molgenis.emx2.web.SwaggerUi.createSwaggerUI;
import static spark.Spark.*;

public class WebApi {
  static Database database;
  static final String APPLICATION_JSON = "application/json";
  static final String SCHEMA = "schema";
  static final String TABLE = "table";

  public WebApi(Database db) {
    database = db;

    port(8080);
    get("/", (request, response) -> "Welcome. Data api available under /data");
    get("/data", WebApi::listSchemas);
    get("/data/:schema", WebApi::listTables);
    get("/data/:schema/openapi.yaml", WebApi::getOpenApiYaml);
    get("/data/:schema/:table/:molgenisid", WebApi::getRow);
    put("/data/:schema/:table", APPLICATION_JSON, WebApi::putRow);
    post("/data/:schema/:table", APPLICATION_JSON, WebApi::postRow);

    // handling of exceptions
    exception(
        JsonException.class,
        (e, req, res) -> {
          res.status(400);
          res.body(
              String.format("{\"message\":\"%s%n%s\"%n}", "Failed to parse JSON:", req.body()));
        });
    exception(
        MolgenisException.class,
        (e, req, res) -> {
          res.status(400);
          res.body(e.getMessage());
        });
  }

  private static String putRow(Request request, Response response) throws MolgenisException {
    Table table = database.getSchema(request.params(SCHEMA)).getTable(request.params(TABLE));
    Row row = JsonRowMapper.jsonToRow(request.body());
    table.update(row);
    response.type(APPLICATION_JSON);
    response.status(200);
    return rowToJson(row);
  }

  public static String getRow(Request request, Response response) throws MolgenisException {
    Table table = database.getSchema(request.params(SCHEMA)).getTable(request.params(TABLE));
    List<Row> rows = table.query().where(MOLGENISID).eq(request.params(MOLGENISID)).retrieve();
    response.status(200);
    return rowToJson(rows.get(0));
  }

  private static String postRow(Request request, Response response) throws MolgenisException {
    Table table = database.getSchema(request.params(SCHEMA)).getTable(request.params(TABLE));
    Row row = JsonRowMapper.jsonToRow(request.body());
    table.insert(row);
    response.status(200);
    response.type(APPLICATION_JSON);
    return rowToJson(row);
  }

  private static String getOpenApiYaml(Request request, Response response)
      throws MolgenisException, IOException {
    Schema schema = database.getSchema(request.params(SCHEMA));
    OpenAPI api = create(schema);
    StringWriter writer = new StringWriter();
    Yaml.pretty().writeValue(writer, api);
    response.status(200);
    return writer.toString();
  }

  private static String listTables(Request request, Response response) {
    response.status(200);
    return createSwaggerUI(request.params(SCHEMA));
  }

  private static String listSchemas(Request request, Response response) throws MolgenisException {
    response.status(200);
    Map<String, String> schemas = new LinkedHashMap<>();
    for (String schemaName : database.getSchemaNames()) {
      schemas.put(schemaName, request.url() + "/" + schemaName);
    }
    return JsonStream.serialize(schemas);
  }
}
