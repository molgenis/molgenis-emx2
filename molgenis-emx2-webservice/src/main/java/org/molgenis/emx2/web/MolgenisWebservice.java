package org.molgenis.emx2.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.JsonException;
import io.swagger.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.MolgenisExport;
import org.molgenis.emx2.io.MolgenisImport;
import org.molgenis.emx2.io.readers.CsvRowReader;
import org.molgenis.emx2.io.readers.CsvRowWriter;
import org.molgenis.emx2.io.emx2.ConvertSchemaToEmx2;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.web.json.JsonMapper;
import org.molgenis.emx2.web.json.JsonRowMapper;
import org.molgenis.emx2.utils.MolgenisException;
import spark.Request;
import spark.Response;
import spark.Spark;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.sql.DataSource;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Stream;

import static org.molgenis.emx2.web.Constants.*;
import static org.molgenis.emx2.web.JsonExceptionMapper.molgenisExceptionToJson;
import static org.molgenis.emx2.web.json.JsonMembersMapper.jsonToMembers;
import static org.molgenis.emx2.web.json.JsonMembersMapper.membersToJson;
import static org.molgenis.emx2.web.json.JsonRowMapper.jsonToRow;
import static org.molgenis.emx2.web.json.JsonSchemaMapper.schemaToJson;
import static spark.Spark.*;

public class MolgenisWebservice {
  // todo look into javalin that claims to have openapi and sparkjava merged together

  public static final String MOLGENIS_TOKEN = "x-molgenis-token";
  public static final String TEMPFILES_DELETE_ON_EXIT = "tempfiles-delete-on-exit";

  private static DataSource dataSource;
  private static Map<String, Database> databaseForRole = new LinkedHashMap<>();
  private static final String SCHEMA = "schema";
  private static final String TABLE = "table";

  private MolgenisWebservice() {
    // hide constructor
  }

  public static void start(DataSource ds) {
    dataSource = ds;
    port(8080);

    // root
    get(
        "/",
        (request, response) ->
            "Welcome to MOLGENIS EMX2 POC.<br/> Data api available under <a href=\"/data\">/data</a><br/>API documentation under <a href=\"/openapi\">/openapi</a>");

    // the data api
    final String dataPath = "/data"; // NOSONAR
    get(dataPath, MolgenisWebservice::schemasGet);
    post(dataPath, MolgenisWebservice::schemasPost);

    // schema level operations: get, add, delete tables + contents
    final String schemaPath = "/data/:schema"; // NOSONAR
    get(schemaPath, ACCEPT_JSON, MolgenisWebservice::tablesMetadataGetJSON);
    get(schemaPath, ACCEPT_CSV, MolgenisWebservice::tablesMetadataGetCSV);
    get(schemaPath, ACCEPT_EXCEL, MolgenisWebservice::tablesMetadataANDdataGetExcel);
    get(schemaPath, ACCEPT_ZIP, MolgenisWebservice::tablesMetadataANDdataGetZip);
    post(schemaPath, MolgenisWebservice::tablesMetadataANDdataPostFile);
    delete(schemaPath, MolgenisWebservice::tablesDelete);

    // table row operations
    final String tablePath = "/data/:schema/:table"; // NOSONAR
    get(tablePath, MolgenisWebservice::rowsGet);
    post(tablePath, ACCEPT_JSON, MolgenisWebservice::rowsPost);
    delete(tablePath, MolgenisWebservice::rowsDelete);

    // schema members operations
    final String membersPath = "/members/:schema"; // NOSONAR
    get(membersPath, MolgenisWebservice::membersGet);
    post(membersPath, MolgenisWebservice::membersPost);
    delete(membersPath, MolgenisWebservice::membersDelete);

    // documentation operations
    get("/openapi", ACCEPT_JSON, MolgenisWebservice::openApiListSchemas);
    get("/openapi/:schema", MolgenisWebservice::openApiUserInterface);
    get("/openapi/:schema/openapi.yaml", MolgenisWebservice::openApiYaml);

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
          res.type(ACCEPT_JSON);
          res.body(molgenisExceptionToJson(e));
        });
  }

  private static String membersDelete(Request request, Response response) throws IOException {
    List<Member> members = jsonToMembers(request.body());
    Schema schema = getAuthenticatedDatabase(request).getSchema(request.params(SCHEMA));
    schema.removeMembers(members);
    response.status(200);
    return "" + members.size();
  }

  private static String membersPost(Request request, Response response) throws IOException {
    List<Member> members = jsonToMembers(request.body());
    Schema schema = getAuthenticatedDatabase(request).getSchema(request.params(SCHEMA));
    schema.addMembers(members);
    response.status(200);
    return "" + members.size();
  }

  private static String membersGet(Request request, Response response)
      throws JsonProcessingException {
    Schema schema = getAuthenticatedDatabase(request).getSchema(request.params(SCHEMA));
    response.status(200);
    response.type(ACCEPT_JSON);
    return membersToJson(schema.getMembers());
  }

  private static String tablesMetadataGetCSV(Request request, Response response)
      throws IOException {
    Schema schema = getAuthenticatedDatabase(request).getSchema(request.params(SCHEMA));
    StringWriter writer = new StringWriter();
    ConvertSchemaToEmx2.toCsv(schema.getMetadata(), writer, ',');
    response.status(200);
    return writer.toString();
  }

  private static String tablesMetadataANDdataGetExcel(Request request, Response response)
      throws IOException {
    Schema schema = getAuthenticatedDatabase(request).getSchema(request.params(SCHEMA));
    Path tempDir = Files.createTempDirectory(TEMPFILES_DELETE_ON_EXIT);
    tempDir.toFile().deleteOnExit();
    try (OutputStream outputStream = response.raw().getOutputStream()) {
      Path excelFile = tempDir.resolve("download.xlsx");
      MolgenisExport.toExcelFile(excelFile, schema);
      outputStream.write(Files.readAllBytes(excelFile));
      response.type("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      response.header(
          "Content-Disposition",
          "attachment; filename="
              + schema.getMetadata().getName()
              + System.currentTimeMillis()
              + ".xlsx");
      return "Export success";
    }
  }

  private static String tablesMetadataGetJSON(Request request, Response response)
      throws IOException {
    Schema schema = getAuthenticatedDatabase(request).getSchema(request.params(SCHEMA));
    String json = schemaToJson(schema.getMetadata());
    response.status(200);
    return json;
  }

  private static String tablesDelete(Request request, Response response) {
    getAuthenticatedDatabase(request).dropSchema(request.params(SCHEMA));
    response.status(200);
    return "Delete schema success";
  }

  private static String schemasPost(Request request, Response response) {
    Row row = jsonToRow(request.body());
    getAuthenticatedDatabase(request).createSchema(row.getString("name"));
    response.status(200);
    return "Create schema success";
  }

  private static String tablesMetadataANDdataGetZip(Request request, Response response)
      throws IOException {

    Path tempDir = Files.createTempDirectory(TEMPFILES_DELETE_ON_EXIT);
    tempDir.toFile().deleteOnExit();
    try (OutputStream outputStream = response.raw().getOutputStream()) {
      Schema schema = getAuthenticatedDatabase(request).getSchema(request.params(SCHEMA));
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

  private static String tablesMetadataANDdataPostFile(Request request, Response response)
      throws IOException, ServletException {
    Schema schema = getAuthenticatedDatabase(request).getSchema(request.params(SCHEMA));
    // get uploaded file
    File tempFile = File.createTempFile(TEMPFILES_DELETE_ON_EXIT, ".tmp");
    tempFile.deleteOnExit();
    request.attribute(
        "org.eclipse.jetty.multipartConfig",
        new MultipartConfigElement(tempFile.getAbsolutePath()));
    try (InputStream input = request.raw().getPart("file").getInputStream()) {
      Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    // depending on file extension use proper importer
    String fileName = request.raw().getPart("file").getSubmittedFileName();
    if (fileName.endsWith(".zip")) {
      MolgenisImport.fromZipFile(tempFile.toPath(), schema);
    } else if (fileName.endsWith(".xlsx")) {
      MolgenisImport.fromExcelFile(tempFile.toPath(), schema);
    } else {
      throw new IOException(
          "File upload failed: extension "
              + fileName.substring(fileName.lastIndexOf('.'))
              + " not supported");
    }

    response.status(200);
    return "Import success";
  }

  private static String rowsGet(Request request, Response response) throws IOException {
    // retrieve data
    Table table =
        getAuthenticatedDatabase(request)
            .getSchema(request.params(SCHEMA))
            .getTable(request.params(TABLE));
    List<Row> rows = table.retrieve();
    // format response
    String accept = request.headers("Accept");
    if (accept == null || accept.toLowerCase().contains(ACCEPT_JSON.toLowerCase())) {
      response.type(ACCEPT_JSON);
      return JsonMapper.rowsToJson(rows);
    }
    if (accept.toLowerCase().contains(ACCEPT_CSV.toLowerCase())) {
      response.type(ACCEPT_CSV);
      StringWriter writer = new StringWriter();
      CsvRowWriter.writeCsv(rows, writer, ',');
      return writer.toString();
    }
    throw new UnsupportedOperationException("unsupported content type: " + request.contentType());
  }

  private static String openApiListSchemas(Request request, Response response) {
    StringBuilder result = new StringBuilder();
    for (String name : getAuthenticatedDatabase(request).getSchemaNames()) {
      result.append("<a href=\"" + request.url() + "/" + name + "\">" + name + "</a><br/>");
    }
    return result.toString();
  }

  private static String rowsPost(Request request, Response response) throws IOException {
    Table table =
        getAuthenticatedDatabase(request)
            .getSchema(request.params(SCHEMA))
            .getTable(request.params(TABLE));
    Iterable<Row> rows = tableRequestBodyToRows(request);
    int count = table.insert(rows);
    response.status(200);
    response.type(ACCEPT_JSON);
    return "" + count;
  }

  private static String rowsDelete(Request request, Response response) throws IOException {
    Table table =
        getAuthenticatedDatabase(request)
            .getSchema(request.params(SCHEMA))
            .getTable(request.params(TABLE));
    Iterable<Row> rows = tableRequestBodyToRows(request);
    int count = table.delete(rows);
    response.status(200);
    response.type(ACCEPT_JSON);
    return "" + count;
  }

  private static Iterable<Row> tableRequestBodyToRows(Request request) throws IOException {
    if (request.contentType().toLowerCase().contains(ACCEPT_JSON.toLowerCase()))
      return JsonRowMapper.jsonToRows(request.body());

    if (request.contentType().toLowerCase().contains(ACCEPT_CSV.toLowerCase()))
      return CsvRowReader.read(new StringReader(request.body()), ',');

    // default
    throw new UnsupportedOperationException("unsupported content type: " + request.contentType());
  }

  private static String openApiYaml(Request request, Response response) throws IOException {
    Schema schema = getAuthenticatedDatabase(request).getSchema(request.params(SCHEMA));
    OpenAPI api = OpenApiForSchemaFactory.createOpenApi(schema.getMetadata());
    response.status(200);
    return Yaml.mapper()
        .configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)
        .writeValueAsString(api);
  }

  private static String openApiUserInterface(Request request, Response response) {
    response.status(200);
    return SwaggerUiFactory.createSwaggerUI(request.params(SCHEMA));
  }

  private static String schemasGet(Request request, Response response) {
    response.status(200);
    Map<String, String> schemas = new LinkedHashMap<>();
    for (String schemaName : getAuthenticatedDatabase(request).getSchemaNames()) {
      schemas.put(schemaName, request.url() + "/" + schemaName);
    }
    return JsonStream.serialize(schemas);
  }

  private static Database getAuthenticatedDatabase(Request request) {
    final String token =
        request.headers(MOLGENIS_TOKEN) == null ? "anonymous" : request.headers(MOLGENIS_TOKEN);

    // we keep a cache of Database instance for each user on this server
    // they share the connection pool

    // todo remove these after a while!!!!

    return databaseForRole.computeIfAbsent(
        token,
        t -> {
          SqlDatabase database;
          database = new SqlDatabase(dataSource);
          database.setActiveUser(token);
          return database;
        });
  }

  public static void stop() {
    Spark.stop();
  }
}
