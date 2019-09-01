package org.molgenis.emx2.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.JsonException;
import io.swagger.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.io.MolgenisExport;
import org.molgenis.emx2.io.MolgenisImport;
import org.molgenis.emx2.io.csv.CsvRowReader;
import org.molgenis.emx2.io.csv.CsvRowWriter;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.web.json.JsonRowMapper;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.utils.MolgenisException;
import spark.Request;
import spark.Response;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Stream;

import static org.molgenis.emx2.web.Constants.*;
import static spark.Spark.*;

public class WebApiFactory {
  private static final String DATA = "/data";
  private static final String DATA_SCHEMA = DATA + "/:schema"; // NOSONAR
  private static final String DATA_SCHEMA_TABLE = DATA_SCHEMA + "/:table"; // NOSONAR
  private static final String DATA_SCHEMA_TABLE_MOLGENISID = DATA_SCHEMA_TABLE + "/:molgenisid";

  private static Database database;
  private static final String SCHEMA = "schema";
  private static final String TABLE = "table";

  private WebApiFactory() {
    // hide constructor
  }

  public static void createWebApi(Database db) {
    database = db;

    port(8080);
    // root
    get(
        "/",
        (request, response) ->
            "Welcome. Data api available under <a href=\"/data\">/data</a> and api documentaiton under <a href=\"/openapi\">/openapi</a>");

    // the data api

    get(DATA, WebApiFactory::schemaGet);

    // documentation
    get("/openapi", ACCEPT_JSON, WebApiFactory::openApiListSchemas);
    get("/openapi/:schema", WebApiFactory::openApiUserInterface);
    get("/openapi/:schema/openapi.yaml", WebApiFactory::openApiYaml);

    // schema operations
    get(DATA_SCHEMA, ACCEPT_JSON, WebApiFactory::openApiListSchemas);
    post(DATA_SCHEMA, WebApiFactory::schemaPostZip);
    get(DATA_SCHEMA, ACCEPT_ZIP, WebApiFactory::schemaGetZip);

    // table operations
    get(DATA_SCHEMA_TABLE, ACCEPT_JSON, WebApiFactory::tableQueryAcceptJSON);
    get(DATA_SCHEMA_TABLE, ACCEPT_CSV, WebApiFactory::tableQueryAcceptCSV);
    post(DATA_SCHEMA_TABLE, WebApiFactory::tablePostOperation);
    delete(DATA_SCHEMA_TABLE, WebApiFactory::tableDeleteOperation);

    // row operations (get rid of those?)
    // get(DATA_SCHEMA_TABLE_MOLGENISID, WebApiFactory::tableGetOperation);

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

  private static String schemaGetZip(Request request, Response response)
      throws MolgenisException, IOException {

    Path tempDir = Files.createTempDirectory("tempfiles-delete-on-exit");
    tempDir.toFile().deleteOnExit();
    try (OutputStream outputStream = response.raw().getOutputStream()) {
      Schema schema = database.getSchema(request.params(SCHEMA));
      Path zipFile = tempDir.resolve("download.zip");
      MolgenisExport.toZipFile(zipFile, schema);
      outputStream.write(Files.readAllBytes(zipFile));
      response.type("application/zip");
      response.header(
          "Content-Disposition",
          "attachment; filename="
              + schema.getMetadata().getName()
              + System.currentTimeMillis()
              + ".zip");
      return "Export success";
    } finally {
      try (Stream<Path> files = Files.walk(tempDir)) {
        files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
      }
    }
  }

  private static String schemaPostZip(Request request, Response response)
      throws MolgenisException, IOException, ServletException {
    Schema schema = database.getSchema(request.params(SCHEMA));
    Path tempFile = getTempFile();
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

  private static Path getTempFile() throws IOException {
    Path tempFile = Files.createTempFile("tempfiles-delete-on-exit", ".tmp");
    tempFile.toFile().deleteOnExit();
    return tempFile;
  }

  private static String tableQueryAcceptJSON(Request request, Response response)
      throws MolgenisException, JsonProcessingException {
    Table table = database.getSchema(request.params(SCHEMA)).getTable(request.params(TABLE));
    List<Row> rows = table.retrieve();
    return JsonMapper.rowsToJson(rows);
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

  //  private static String tableGetOperation(Request request, Response response)
  //      throws MolgenisException, JsonProcessingException {
  //    Table table = database.getSchema(request.params(SCHEMA)).getTable(request.params(TABLE));
  //
  //    List<Row> rows = table.query().where(MOLGENISID).eq(request.params(MOLGENISID)).retrieve();
  //    response.status(200);
  //    return JsonMapper.rowToJson(rows.get(0));
  //  }

  private static String tablePostOperation(Request request, Response response)
      throws MolgenisException, IOException {
    Table table = database.getSchema(request.params(SCHEMA)).getTable(request.params(TABLE));
    Iterable<Row> rows = tableRequestBodyToRows(request);
    int count = table.insert(rows);
    response.status(200);
    response.type(ACCEPT_JSON);
    return "" + count;
  }

  private static String tableDeleteOperation(Request request, Response response)
      throws MolgenisException, IOException {
    Table table = database.getSchema(request.params(SCHEMA)).getTable(request.params(TABLE));
    Iterable<Row> rows = tableRequestBodyToRows(request);
    int count = table.delete(rows);
    response.status(200);
    response.type(ACCEPT_JSON);
    return "" + count;
  }

  private static Iterable<Row> tableRequestBodyToRows(Request request) throws IOException {
    Iterable<Row> rows;
    switch (request.contentType()) {
      case ACCEPT_JSON:
        rows = JsonRowMapper.jsonToRows(request.body());
        break;
      case ACCEPT_CSV:
        rows = CsvRowReader.read(new StringReader(request.body()));
        break;
      default:
        throw new UnsupportedOperationException(
            "unsupported content type: " + request.contentType());
    }
    return rows;
  }

  private static String openApiYaml(Request request, Response response)
      throws MolgenisException, IOException {
    Schema schema = database.getSchema(request.params(SCHEMA));
    OpenAPI api = OpenApiForSchemaFactory.createOpenApi(schema.getMetadata());
    StringWriter writer = new StringWriter();
    Yaml.pretty().writeValue(writer, api);
    response.status(200);
    return writer.toString();
  }

  private static String openApiUserInterface(Request request, Response response) {
    response.status(200);
    return SwaggerUiFactory.createSwaggerUI(request.params(SCHEMA));
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
