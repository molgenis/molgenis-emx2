package org.molgenis.emx2.web;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.jsoniter.spi.JsonException;
import io.swagger.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.utils.MolgenisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Spark;

import javax.sql.DataSource;
import java.io.*;
import java.util.*;

import static org.molgenis.emx2.web.Constants.*;
import static org.molgenis.emx2.web.json.JsonExceptionMapper.molgenisExceptionToJson;
import static spark.Spark.*;

public class MolgenisWebservice {
  public static final String MOLGENIS_TOKEN = "x-molgenis-token";
  public static final String TEMPFILES_DELETE_ON_EXIT = "tempfiles-delete-on-exit";
  public static final Logger logger = LoggerFactory.getLogger(MolgenisWebservice.class);

  private static DataSource dataSource;
  private static Map<String, Database> databaseForRole = new LinkedHashMap<>();
  static final String SCHEMA = "schema";
  public static final String TABLE = "table";

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

    JsonApi.create();
    CsvApi.create();
    MembersApi.create();
    ZipApi.create();
    ExcelApi.create();
    GraphqlApi.createGraphQLSchema();

    // schema members operations

    // documentation operations
    get("/openapi", ACCEPT_JSON, MolgenisWebservice::openApiListSchemas);
    get("/openapi/:schema", MolgenisWebservice::openApiUserInterface);
    get("/openapi/:schema/openapi.yaml", MolgenisWebservice::openApiYaml);

    // handling of exceptions
    exception(
        JsonException.class,
        (e, req, res) -> {
          logger.debug(e.toString());
          res.status(400);
          res.body(
              String.format("{\"message\":\"%s%n%s\"%n}", "Failed to parse JSON:", req.body()));
        });
    exception(
        MolgenisException.class,
        (e, req, res) -> {
          logger.debug(e.toString());
          res.status(400);
          res.type(ACCEPT_JSON);
          res.body(molgenisExceptionToJson(e));
        });
  }

  private static String openApiListSchemas(Request request, Response response) {
    StringBuilder result = new StringBuilder();
    for (String name : getAuthenticatedDatabase(request).getSchemaNames()) {
      result.append("<a href=\"" + request.url() + "/" + name + "\">" + name + "</a><br/>");
    }
    return result.toString();
  }

  private static String openApiYaml(Request request, Response response) throws IOException {
    Schema schema = getAuthenticatedDatabase(request).getSchema(request.params(SCHEMA));
    OpenAPI api = OpenApiYamlGenerator.createOpenApi(schema.getMetadata());
    response.status(200);
    return Yaml.mapper()
        .configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)
        .writeValueAsString(api);
  }

  private static String openApiUserInterface(Request request, Response response) {
    response.status(200);
    return OpenApiUiFactory.createSwaggerUI(request.params(SCHEMA));
  }

  static synchronized Database getAuthenticatedDatabase(Request request) {
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
          // database.setActiveUser(token);
          return database;
        });
  }

  public static Table getTable(Request request) {
    return getAuthenticatedDatabase(request)
        .getSchema(request.params(SCHEMA))
        .getTable(request.params(TABLE));
  }

  public static void stop() {
    Spark.stop();
  }
}
