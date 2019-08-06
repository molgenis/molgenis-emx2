package org.molgenis.emx2.web;

import com.jsoniter.JsonIterator;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.JsonException;
import io.swagger.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import org.molgenis.*;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
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

  public WebApi(Database database) {
    this.database = database;

    final String ACCEPT_JSON = "application/json";

    port(8080);
    get("/", (request, response) -> "Welcome. Data api available under /data");
    get("/data", WebApi::listSchemas);
    get("/data/:schema", WebApi::listTables);
    get("/data/:schema/openapi.yaml", WebApi::getOpenApiYaml);
    get("/data/:schema/:table/:molgenisid", WebApi::getRow);
    post("/data/:schema/:table", ACCEPT_JSON, WebApi::postRow);

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
          res.body(e.getMessage()); // todo make nice format
        });
  }

  public static String getRow(Request request, Response response) throws MolgenisException {
    Table table = database.getSchema(request.params("schema")).getTable(request.params("table"));
    List<Row> rows = table.query().where(MOLGENISID).eq(request.params(MOLGENISID)).retrieve();
    response.status(200);
    return rowToJson(rows.get(0));
  }

  private static String postRow(Request request, Response response) throws MolgenisException {
    Table table = database.getSchema(request.params("schema")).getTable(request.params("table"));
    Row row = JsonRowMapper.jsonToRow(request.body());
    table.insert(row);
    response.status(200);
    response.type("application/json");
    return rowToJson(row);
  }

  private static String getOpenApiYaml(Request request, Response response)
      throws MolgenisException, IOException {
    Schema schema = database.getSchema(request.params("schema"));
    OpenAPI api = create(schema);
    StringWriter writer = new StringWriter();
    Yaml.pretty().writeValue(writer, api);
    return writer.toString();
  }

  private static String listTables(Request request, Response response)
      throws MolgenisException, UnsupportedEncodingException {
    return createSwaggerUI(request.params("schema"));
  }

  private static String listSchemas(Request request, Response response)
      throws MolgenisException, MalformedURLException {
    response.status(200);
    Map<String, String> schemas = new LinkedHashMap<>();
    for (String schemaName : database.getSchemaNames()) {
      schemas.put(schemaName, request.url() + "/" + schemaName);
    }
    return JsonStream.serialize(schemas);
  }
}
