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
import org.molgenis.emx2.io.emx2format.ConvertSchemaToEmx2;
import org.molgenis.emx2.web.json.JsonMapper;
import org.molgenis.emx2.web.json.JsonRowMapper;
import org.molgenis.emx2.utils.MolgenisException;
import spark.Request;
import spark.Response;
import spark.Spark;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Stream;

import static org.molgenis.emx2.web.Constants.*;
import static org.molgenis.emx2.web.json.JsonMembersMapper.jsonToMembers;
import static org.molgenis.emx2.web.json.JsonMembersMapper.membersToJson;
import static org.molgenis.emx2.web.json.JsonRowMapper.jsonToRow;
import static org.molgenis.emx2.web.json.JsonSchemaMapper.schemaToJson;
import static spark.Spark.*;

public class MolgenisWebservice {
  // todo look into javalin that claims to have openapi and sparkjava merged together

  private static final String DATA = "/data";
  private static final String DATA_SCHEMA = DATA + "/:schema"; // NOSONAR
  private static final String DATA_SCHEMA_TABLE = DATA_SCHEMA + "/:table"; // NOSONAR

  private static Database database;
  private static Map<String, Database> databaseForRole = new LinkedHashMap<>();
  private static final String SCHEMA = "schema";
  private static final String TABLE = "table";

  private MolgenisWebservice() {
    // hide constructor
  }

  public static void start(Database db) {
    database = db;

    port(8080);

    //    before( // in case of trailing slash, remove that slash
    //        (req, res) -> {
    //          String path = req.pathInfo();
    //          if (path.endsWith("/")) res.redirect(path.substring(0, path.length() - 1));
    //        });

    // root
    get(
        "/",
        (request, response) ->
            "Welcome to MOLGENIS EMX2 POC.<br/> Data api available under <a href=\"/data\">/data</a><br/>API documentation under <a href=\"/openapi\">/openapi</a>");

    // aut api

    // the data api
    get(DATA, MolgenisWebservice::apiGet);
    post(DATA, MolgenisWebservice::schemaPost);
    delete(DATA, MolgenisWebservice::schemaDelete);
    // documentation
    get("/openapi", ACCEPT_JSON, MolgenisWebservice::openApiListSchemas);
    get("/openapi/:schema", MolgenisWebservice::openApiUserInterface);
    get("/openapi/:schema/openapi.yaml", MolgenisWebservice::openApiYaml);

    // schema operations
    get(DATA_SCHEMA, ACCEPT_JSON, MolgenisWebservice::schemaGetJson);
    get(DATA_SCHEMA, ACCEPT_CSV, MolgenisWebservice::schemaGetCsv);
    get(DATA_SCHEMA, ACCEPT_EXCEL, MolgenisWebservice::schemaGetExcel);
    get(DATA_SCHEMA, ACCEPT_ZIP, MolgenisWebservice::schemaGetZip);
    post(DATA_SCHEMA, MolgenisWebservice::schemaPostZip);
    delete(DATA_SCHEMA, MolgenisWebservice::schemaDelete);

    get("/admin/:schema/members", MolgenisWebservice::membersGet);
    post("/admin/:schema/members", MolgenisWebservice::membersPost);

    // table operations
    get(DATA_SCHEMA_TABLE, MolgenisWebservice::tableGet);
    post(DATA_SCHEMA_TABLE, ACCEPT_JSON, MolgenisWebservice::tablePost);
    delete(DATA_SCHEMA_TABLE, MolgenisWebservice::tableDelete);

    // row operations (get rid of those?)

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

  private static String membersPost(Request request, Response response)
      throws MolgenisException, IOException {
    List<Member> members = jsonToMembers(request.body());
    Schema schema = database.getSchema(request.params(SCHEMA));
    schema.addMembers(members);
    response.status(200);
    return "" + members.size();
  }

  private static String membersGet(Request request, Response response)
      throws MolgenisException, JsonProcessingException {
    Schema schema = database.getSchema(request.params(SCHEMA));
    response.status(200);
    return membersToJson(schema.getMembers());
  }

  private static String schemaGetCsv(Request request, Response response)
      throws MolgenisException, IOException {
    Schema schema = database.getSchema(request.params(SCHEMA));
    StringWriter writer = new StringWriter();
    ConvertSchemaToEmx2.toCsv(schema.getMetadata(), writer);
    response.status(200);
    return writer.toString();
  }

  private static String schemaGetExcel(Request request, Response response)
      throws MolgenisException, IOException {
    Schema schema = getAuthenticatedDatabase(request).getSchema(request.params(SCHEMA));
    Path tempDir = Files.createTempDirectory("tempfiles-delete-on-exit");
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

  private static String schemaGetJson(Request request, Response response)
      throws MolgenisException, IOException {
    Schema schema = getAuthenticatedDatabase(request).getSchema(request.params(SCHEMA));
    String json = schemaToJson(schema.getMetadata());
    response.status(200);
    return json;
  }

  private static String schemaDelete(Request request, Response response) throws MolgenisException {
    database.dropSchema(request.params(SCHEMA));
    response.status(200);
    return "Delete schema success";
  }

  private static String schemaPost(Request request, Response response) throws MolgenisException {
    Row row = jsonToRow(request.body());
    // todo validate
    database.createSchema(row.getString("name"));
    response.status(200);
    return "Create schema success";
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
    // get uploaded file
    File tempFile = File.createTempFile("tempfiles-delete-on-exit", ".tmp");
    tempFile.deleteOnExit();
    request.attribute(
        "org.eclipse.jetty.multipartConfig",
        new MultipartConfigElement(tempFile.getAbsolutePath().toString()));
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
              + fileName.substring(fileName.lastIndexOf("."))
              + " not supported");
    }

    response.status(200);
    return "Import success";
  }

  private static Path getTempFile() throws IOException {
    Path tempFile = Files.createTempFile("tempfiles-delete-on-exit", ".tmp");
    tempFile.toFile().deleteOnExit();
    return tempFile;
  }

  private static String tableGet(Request request, Response response)
      throws MolgenisException, IOException {
    // retrieve data
    Table table = database.getSchema(request.params(SCHEMA)).getTable(request.params(TABLE));
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
      CsvRowWriter.writeCsv(rows, writer);
      return writer.toString();
    }
    throw new UnsupportedOperationException("unsupported content type: " + request.contentType());
  }

  private static String openApiListSchemas(Request request, Response response)
      throws MolgenisException {
    StringBuilder result = new StringBuilder();
    for (String name : database.getSchemaNames()) {
      result.append("<a href=\"" + request.url() + "/" + name + "\">" + name + "</a><br/>");
    }
    return result.toString();
  }

  private static String tablePost(Request request, Response response)
      throws MolgenisException, IOException {
    Table table = database.getSchema(request.params(SCHEMA)).getTable(request.params(TABLE));
    Iterable<Row> rows = tableRequestBodyToRows(request);
    int count = table.insert(rows);
    response.status(200);
    response.type(ACCEPT_JSON);
    return "" + count;
  }

  private static String tableDelete(Request request, Response response)
      throws MolgenisException, IOException {
    Table table = database.getSchema(request.params(SCHEMA)).getTable(request.params(TABLE));
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
      return CsvRowReader.read(new StringReader(request.body()));

    // default
    throw new UnsupportedOperationException("unsupported content type: " + request.contentType());
  }

  private static String openApiYaml(Request request, Response response)
      throws MolgenisException, IOException {
    Schema schema = database.getSchema(request.params(SCHEMA));
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

  private static String apiGet(Request request, Response response) throws MolgenisException {
    response.status(200);
    Map<String, String> schemas = new LinkedHashMap<>();
    for (String schemaName : database.getSchemaNames()) {
      schemas.put(schemaName, request.url() + "/" + schemaName);
    }
    return JsonStream.serialize(schemas);
  }

  private static Database getAuthenticatedDatabase(Request request) throws MolgenisException {
    return database;

    //    String token = request.headers("x-molgenis-token");
    //
    //    // for testing, we use token == role, and give connection in a map
    //    // of course this leaves open connections
    //
    //    if (databaseForRole.get(token) == null) {
    //      Connection conn = null;
    //      try {
    //        conn =
    //            DriverManager.getConnection(
    //                "jdbc:postgresql://localhost:5432/molgenis", "molgenis", "molgenis");
    //      } catch (SQLException sqle) {
    //        throw new MolgenisException("connection faile", "conenction failed",
    // sqle.getMessage());
    //      }
    //      try (Statement stmt = conn.createStatement()) {
    //        stmt.execute("set role \"" + token + "\"");
    //      } catch (SQLException sqle) {
    //        throw new MolgenisException(
    //            "invalid_token", "Invalid token", "Role " + token + " not known" +
    // sqle.getMessage());
    //      }
    //      databaseForRole.put(token, new SqlDatabase(conn));
    //    }
    //    return databaseForRole.get(token);
  }

  public static void stop() {
    Spark.stop();
  }
}
