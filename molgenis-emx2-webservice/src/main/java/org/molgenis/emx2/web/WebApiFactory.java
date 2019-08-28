package org.molgenis.emx2.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.JsonException;
import io.swagger.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import org.molgenis.*;
import org.molgenis.data.Database;
import org.molgenis.data.Row;
import org.molgenis.data.Table;
import org.molgenis.emx2.io.MolgenisImport;
import org.molgenis.emx2.io.csv.CsvRowWriter;
import org.molgenis.data.Schema;
import org.molgenis.emx2.web.json.JsonRowMapper;
import org.molgenis.metadata.SchemaMetadata;
import org.molgenis.metadata.TableMetadata;
import spark.Request;
import spark.Response;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.molgenis.data.Row.MOLGENISID;
import static org.molgenis.emx2.web.JsonMapper.rowToJson;
import static org.molgenis.emx2.web.JsonMapper.rowsToJson;
import static org.molgenis.emx2.web.OpenApiForSchemaFactory.createOpenApi;
import static org.molgenis.emx2.web.SwaggerUiFactory.createSwaggerUI;
import static spark.Spark.*;

public class WebApiFactory {
  private static final String DATA_SCHEMA = "/data/:schema"; // NOSONAR
  private static final String DATA_SCHEMA_TABLE = DATA_SCHEMA + "/:table"; // NOSONAR
  private static final String DATA_SCHEMA_TABLE_MOLGENISID = DATA_SCHEMA_TABLE + "/:molgenisid";

  private static Database database;
  private static final String ACCEPT_JSON = "application/json";
  private static final String ACCEPT_CSV = "text/csv";

  private static final String SCHEMA = "schema";
  private static final String TABLE = "table";

  private WebApiFactory() {
    // hide constructor
  }

  public static void createWebApi(Database db) {
    database = db;

    port(8080);
    get(
        "/",
        (request, response) ->
            "Welcome. Data api available under <a href=\"/data\">/data</a> and api documentaiton under <a href=\"/openapi\">/openapi</a>");
    get("/data", WebApiFactory::schemaGet);

    // documentation
    get("/openapi", ACCEPT_JSON, WebApiFactory::openApiListSchemas);
    get("/openapi/:schema", WebApiFactory::openApiUserInterface);
    get("/openapi/:schema/openapi.yaml", WebApiFactory::openApiYaml);

    // actual api
    get(DATA_SCHEMA, ACCEPT_JSON, WebApiFactory::openApiListSchemas);
    post(DATA_SCHEMA, WebApiFactory::schemaPostZip);

    get(DATA_SCHEMA_TABLE, ACCEPT_JSON, WebApiFactory::tableQueryAcceptJSON);
    get(DATA_SCHEMA_TABLE, ACCEPT_CSV, WebApiFactory::tableQueryAcceptCSV);
    post(DATA_SCHEMA_TABLE, ACCEPT_JSON, WebApiFactory::tablePostOperation);
    put(DATA_SCHEMA_TABLE, ACCEPT_JSON, WebApiFactory::rowPutOperation);

    get(DATA_SCHEMA_TABLE_MOLGENISID, WebApiFactory::tableGetOperation);

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

  private static String schemaPostZip(Request request, Response response)
      throws MolgenisException, IOException, ServletException {
    Schema schema = database.getSchema(request.params(SCHEMA));
    Path tempFile = Files.createTempFile("tempfiles-delete-on-exit", ".tmp");
    tempFile.toFile().deleteOnExit();
    request.attribute(
        "org.eclipse.jetty.multipartConfig",
        new MultipartConfigElement(tempFile.toAbsolutePath().toString()));
    try (InputStream input = request.raw().getPart("file").getInputStream()) {
      Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);
    }
    MolgenisImport.fromZipFile(tempFile, schema);
    response.status(200);
    return "Import success";
  }

  private static String tableQueryAcceptJSON(Request request, Response response)
      throws MolgenisException, JsonProcessingException {
    Table table = database.getSchema(request.params(SCHEMA)).getTable(request.params(TABLE));
    List<Row> rows = table.retrieve();
    return rowsToJson(rows);
  }

  private static String tableQueryAcceptCSV(Request request, Response response)
      throws MolgenisException, IOException {
    Table table = database.getSchema(request.params(SCHEMA)).getTable(request.params(TABLE));
    List<Row> rows = table.retrieve();
    StringWriter writer = new StringWriter();
    CsvRowWriter.writeCsv(rows, writer);
    return writer.toString();
  }

  private static String openApiListSchemas(Request request, Response response)
      throws MolgenisException, JsonProcessingException {
    SchemaMetadata schema = database.getSchema(request.params(SCHEMA)).getMetadata();
    List<TableMetadata> result = new ArrayList<>();
    for (String name : schema.getTableNames()) {
      result.add(schema.getTableMetadata(name));
    }
    return JsonMapper.schemaToJson(schema);
  }

  private static String rowPutOperation(Request request, Response response)
      throws MolgenisException, JsonProcessingException {
    Table table = database.getSchema(request.params(SCHEMA)).getTable(request.params(TABLE));
    Row row = JsonRowMapper.jsonToRow(request.body());
    table.update(row);
    response.type(ACCEPT_JSON);
    response.status(200);
    return rowToJson(row);
  }

  private static String tableGetOperation(Request request, Response response)
      throws MolgenisException, JsonProcessingException {
    Table table = database.getSchema(request.params(SCHEMA)).getTable(request.params(TABLE));
    List<Row> rows = table.query().where(MOLGENISID).eq(request.params(MOLGENISID)).retrieve();
    response.status(200);
    return rowToJson(rows.get(0));
  }

  private static String tablePostOperation(Request request, Response response)
      throws MolgenisException, JsonProcessingException {
    Table table = database.getSchema(request.params(SCHEMA)).getTable(request.params(TABLE));
    Row row = JsonRowMapper.jsonToRow(request.body());
    table.insert(row);
    response.status(200);
    response.type(ACCEPT_JSON);
    return rowToJson(row);
  }

  private static String openApiYaml(Request request, Response response)
      throws MolgenisException, IOException {
    Schema schema = database.getSchema(request.params(SCHEMA));
    OpenAPI api = createOpenApi(schema.getMetadata());
    StringWriter writer = new StringWriter();
    Yaml.pretty().writeValue(writer, api);
    response.status(200);
    return writer.toString();
  }

  private static String openApiUserInterface(Request request, Response response) {
    response.status(200);
    return createSwaggerUI(request.params(SCHEMA));
  }

  private static String schemaGet(Request request, Response response) throws MolgenisException {
    response.status(200);
    Map<String, String> schemas = new LinkedHashMap<>();
    for (String schemaName : database.getSchemaNames()) {
      schemas.put(schemaName, request.url() + "/" + schemaName);
    }
    return JsonStream.serialize(schemas);
  }
}
