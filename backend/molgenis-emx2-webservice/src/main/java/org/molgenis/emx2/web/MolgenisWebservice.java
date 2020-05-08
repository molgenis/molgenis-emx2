package org.molgenis.emx2.web;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.jsoniter.spi.JsonException;
import io.swagger.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import org.molgenis.emx2.*;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.web.graphql.GraphqlApi;
import org.molgenis.emx2.web.graphql.GraphqlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Spark;

import javax.sql.DataSource;
import java.io.*;
import java.util.Properties;

import static org.molgenis.emx2.web.Constants.*;
import static org.molgenis.emx2.web.json.JsonExceptionMapper.molgenisExceptionToJson;
import static spark.Spark.*;

public class MolgenisWebservice {
  static final String TEMPFILES_DELETE_ON_EXIT = "tempfiles-delete-on-exit";
  static final Logger logger = LoggerFactory.getLogger(MolgenisWebservice.class);
  public static final String SCHEMA = "schema";
  static MolgenisSessionManager sessionManager;
  static String version = "undefined";

  private MolgenisWebservice() {
    // hide constructor
  }

  public static void start(DataSource ds) throws IOException {
    final Properties properties = new Properties();
    //    properties.load(
    //        MolgenisWebservice.class.getClassLoader().getResourceAsStream("version.properties"));
    //    version = properties.getProperty("emx2.version");
    //    logger.info("Starting EMX2 version: " + version);

    sessionManager = new MolgenisSessionManager(ds);
    port(8080);

    staticFiles.location("/public_html");

    // root
    get(
        "/",
        ACCEPT_HTML,
        (request, response) ->
            "Welcome to MOLGENIS EMX2 data api service POC version "
                + version
                + ".<br/>. See <a href=\"/api/\">/api/</a> and  <a href=\"/apps/central/\">/apps/central/</a>");

    redirect.get("/api", "/api/");
    get(
        "/api/",
        ACCEPT_HTML,
        (request, response) -> {
          return "Welcome to MOLGENIS EMX2 POC <br/>" + listSchemas(request, response);
        });

    // services (matched in order of creation)
    AppsProxyService.create(new SqlDatabase(ds));
    JsonApi.create();
    CsvApi.create();
    MembersApi.create();
    ZipApi.create();
    ExcelApi.create();
    GraphqlApi.createGraphQLservice(sessionManager);
    GroupPathMapper.create();

    // schema members operations

    // documentation operations
    get("/api/openapi", ACCEPT_JSON, MolgenisWebservice::listSchemas);
    get("/api/openapi/:schema", OpenApiUiFactory::getOpenApiUserInterface);
    get("/api/openapi/:schema/openapi.yaml", MolgenisWebservice::openApiYaml);

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
          logger.error(e.getMessage());
          res.status(400);
          res.type(ACCEPT_JSON);
          res.body(molgenisExceptionToJson(e));
        });
    exception(
        GraphqlException.class,
        (e, req, res) -> {
          logger.error(e.getMessage());
          res.status(400);
          res.type(ACCEPT_JSON);
          res.body(molgenisExceptionToJson(e));
        });

    // after handle session changes
    afterAfter(sessionManager::updateSession);
  }

  public static void stop() {
    Spark.stop();
  }

  private static String listSchemas(Request request, Response response) {
    StringBuilder result = new StringBuilder();
    result.append("Schema independent API:");
    result.append(
        "graphql: <a href=\"/api/graphql/\">/api/graphql    </a> <a href=\"/api/playground.html?schema=/api/graphql\">playground</a>");

    result.append("<p/>Schema APIs:<ul>");
    for (String name : sessionManager.getSession(request).getDatabase().getSchemaNames()) {
      result.append("<li>" + name);
      result.append(" <a href=\"/api/openapi/" + name + "\">openapi</a>");
      result.append(
          " graphql: <a href=\"/api/graphql/"
              + name
              + "\">endpoint</a> <a href=\"/api/playground.html?schema=/api/graphql/"
              + name
              + "\">playground</a>");
      result.append("</li>");
    }
    result.append("</ul>");
    return result.toString();
  }

  private static String openApiYaml(Request request, Response response) throws IOException {
    OpenAPI api = OpenApiYamlGenerator.createOpenApi(getSchema(request).getMetadata());
    response.status(200);
    return Yaml.mapper()
        .configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)
        .writeValueAsString(api);
  }

  public static String sanitize(String string) {
    if (string != null) return string.replaceAll("[\n|\r|\t]", "_");
    else return null;
  }

  /** get database either from session or based on token */
  // helper method used in multiple places
  public static Table getTable(Request request) {
    return sessionManager
        .getSession(request)
        .getDatabase()
        .getSchema(sanitize(request.params(SCHEMA)))
        .getTable(sanitize(request.params(TABLE)));
  }

  public static Schema getSchema(Request request) {
    return sessionManager
        .getSession(request)
        .getDatabase()
        .getSchema(sanitize(request.params(SCHEMA)));
  }
}
