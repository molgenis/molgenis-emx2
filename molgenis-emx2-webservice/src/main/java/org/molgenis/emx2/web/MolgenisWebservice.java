package org.molgenis.emx2.web;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.jsoniter.spi.JsonException;
import io.swagger.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.MolgenisException;
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
  static final String MOLGENIS_TOKEN = "x-molgenis-token";
  static final String TEMPFILES_DELETE_ON_EXIT = "tempfiles-delete-on-exit";
  static final Logger logger = LoggerFactory.getLogger(MolgenisWebservice.class);

  private static DataSource dataSource;
  private static Map<String, Database> databaseForRole = new LinkedHashMap<>();
  static final String SCHEMA = "schema";

  private MolgenisWebservice() {
    // hide constructor
  }

  public static void start(DataSource ds) {
    dataSource = ds;
    port(8080);

    staticFiles.location("/public_html");

    // root
    get(
        "/",
        (request, response) ->
            "Welcome to MOLGENIS EMX2 POC.<br/>" + listSchemas(request, response));

    JsonApi.create();
    CsvApi.create();
    MembersApi.create();
    ZipApi.create();
    ExcelApi.create();
    GraphqlApi.createGraphQLservice();

    // schema members operations

    // documentation operations
    get("/openapi", ACCEPT_JSON, MolgenisWebservice::listSchemas);
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

  private static String listSchemas(Request request, Response response) {
    StringBuilder result = new StringBuilder();
    result.append("Schema independent API:");
    result.append(
        "graphql: <a href=\"/api/graphql/\">/api/graphql/</a> <a href=\"playground.html?schema=/api/graphql\">playground</a>");

    result.append("<p/>Schema APIs:<ul>");
    for (String name : getAuthenticatedDatabase(request).getSchemaNames()) {
      result.append("<li>" + name);
      result.append(" <a href=\"openapi/" + name + "\">openapi</a>");
      result.append(
          " graphql: <a href=\"/api/graphql/"
              + name
              + "\">endpoint</a> <a href=\"playground.html?schema=/api/graphql/"
              + name
              + "\">playground</a>");
      result.append("</li>");
    }
    result.append("</ul>");
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

  /** get database either from session or based on token */
  static synchronized Database getAuthenticatedDatabase(Request request) {

    // already in a session, then return that
    if (request.session().attribute("database") != null) {
      return request.session().attribute("database");
    }

    // otherwise try tokens
    final String token =
        request.headers(MOLGENIS_TOKEN) == null ? "anonymous" : request.headers(MOLGENIS_TOKEN);

    // todo remove cached after a while!!!!
    return databaseForRole.computeIfAbsent(
        token,
        t -> {
          SqlDatabase database;
          database = new SqlDatabase(dataSource);
          database.setActiveUser(token);
          return database;
        });
  }

  // helper method used in multiple places
  public static Table getTable(Request request) {
    return getAuthenticatedDatabase(request)
        .getSchema(request.params(SCHEMA))
        .getTable(request.params(TABLE));
  }

  public static void stop() {
    Spark.stop();
  }
}
