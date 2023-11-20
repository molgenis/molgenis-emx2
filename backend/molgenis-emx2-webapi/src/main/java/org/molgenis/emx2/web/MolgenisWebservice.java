package org.molgenis.emx2.web;

import static org.molgenis.emx2.Constants.OIDC_CALLBACK_PATH;
import static org.molgenis.emx2.Constants.OIDC_LOGIN_PATH;
import static org.molgenis.emx2.json.JsonExceptionMapper.molgenisExceptionToJson;
import static org.molgenis.emx2.web.Constants.*;
import static spark.Spark.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.molgenis.emx2.*;
import org.molgenis.emx2.web.controllers.OIDCController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Spark;

public class MolgenisWebservice {
  public static final String SCHEMA = "schema";
  public static final String EDITOR = "Editor";
  public static final String MANAGER = "Manager";
  public static final String ROLE = "role";
  public static final String VIEWER = "Viewer";
  static final String TEMPFILES_DELETE_ON_EXIT = "tempfiles-delete-on-exit";
  static final Logger logger = LoggerFactory.getLogger(MolgenisWebservice.class);
  private static final String ROBOTS_TXT = "robots.txt";
  private static final String USER_AGENT_ALLOW = "User-agent: *\nAllow: /";
  static MolgenisSessionManager sessionManager;
  static OIDCController oidcController;

  private MolgenisWebservice() {
    // hide constructor
  }

  public static void start(int port) {

    sessionManager = new MolgenisSessionManager();
    oidcController = new OIDCController(sessionManager, new SecurityConfigFactory().build());
    port(port);

    staticFiles.location("/public_html");

    /*
     * WARNING !! SPARK JAVA USES DESIGN WHERE THE ORDER OF REQUEST DEFINITION DETERMINES THE HANDLER
     */

    MessageApi.create();

    get(
        ("/" + OIDC_CALLBACK_PATH),
        (request, response) -> oidcController.handleLoginCallback(request, response));
    get(("/" + OIDC_LOGIN_PATH), oidcController::handleLoginRequest);
    get("/" + ROBOTS_TXT, MolgenisWebservice::robotsDotTxt);

    // get setting for home
    get(
        "/",
        ACCEPT_HTML,
        (request, response) -> {
          // check for setting
          String ladingPagePath =
              sessionManager.getSession(request).getDatabase().getSetting(LANDING_PAGE);
          if (ladingPagePath != null) {
            response.redirect(ladingPagePath);
          } else {
            response.redirect("/apps/central/");
          }
          return response;
        });

    redirect.get("/api", "/api/");

    get(
        "/api/",
        ACCEPT_HTML,
        (request, response) ->
            "Welcome to MOLGENIS EMX2 POC <br/>" + listSchemas(request, response));
    // documentation operations
    get("/api/openapi", ACCEPT_JSON, MolgenisWebservice::listSchemas);
    // docs per schema
    get("/:schema/api/openapi", OpenApiUiFactory::getOpenApiUserInterface);
    get("/:schema/api/openapi.yaml", MolgenisWebservice::openApiYaml);
    get(
        "/:schema/api",
        (request, response) -> "Welcome to schema api. Check <a href=\"api/openapi\">openapi</a>");

    SiteMapService.create();
    CsvApi.create();
    ZipApi.create();
    ExcelApi.create();
    FileApi.create();
    JsonYamlApi.create();
    TaskApi.create();
    GraphqlApi.createGraphQLservice(sessionManager);
    RDFApi.create(sessionManager);
    GraphGenomeApi.create(sessionManager);
    BeaconApi.create(sessionManager);
    FAIRDataPointApi.create(sessionManager);
    BootstrapThemeService.create();

    get(
        "/:schema",
        (req, res) -> {
          final String redirectLocation = "/" + req.params(SCHEMA) + "/";
          logger.debug(
              String.format(
                  "handle '/:schema' redirect: from: %s to: %s", req.pathInfo(), redirectLocation));
          res.redirect(redirectLocation);
          return "";
        });

    get("/:schema/", MolgenisWebservice::redirectSchemaToFirstMenuItem);

    // greedy proxy stuff, always put last!
    GroupPathMapper.create();

    // schema members operations

    // handling of exceptions
    exception(
        Exception.class,
        (e, req, res) -> {
          logger.error(e.getMessage(), e);
          res.status(400);
          res.type(ACCEPT_JSON);
          res.body(molgenisExceptionToJson(e));
        });
  }

  private static String robotsDotTxt(Request request, Response response) {
    response.type("text/plain;charset=UTF-8");
    return USER_AGENT_ALLOW;
  }

  private static String redirectSchemaToFirstMenuItem(Request request, Response response) {
    try {
      Schema schema = getSchema(request);
      if (schema == null) {
        throw new MolgenisException("Cannot redirectSchemaToFirstMenuItem, schema is null");
      }
      String role = schema.getRoleForActiveUser();
      Optional<String> menuSettingValue = schema.getMetadata().findSettingValue("menu");
      if (menuSettingValue.isPresent()) {
        List<Map<String, String>> menu =
            new ObjectMapper().readValue(menuSettingValue.get(), List.class);
        menu =
            menu.stream()
                .filter(
                    el ->
                        role == null
                            || role.equals(schema.getDatabase().getAdminUserName())
                            || el.get(ROLE) == null
                            || el.get(ROLE).equals(VIEWER)
                                && List.of(VIEWER, EDITOR, MANAGER).contains(role)
                            || el.get(ROLE).equals(EDITOR)
                                && List.of(EDITOR, MANAGER).contains(role)
                            || el.get(ROLE).equals(MANAGER) && role.equals(MANAGER))
                .toList();
        if (!menu.isEmpty()) {
          response.redirect(
              "/" + request.params(SCHEMA) + "/" + menu.get(0).get("href").replace("../", ""));
        }
      }

    } catch (Exception e) {
      // silly default
      logger.debug(e.getMessage());
    }
    response.redirect("/" + request.params(SCHEMA) + "/tables");
    return "";
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
      result.append(" <a href=\"/" + name + "/api/openapi\">openapi</a>");
      result.append(
          " graphql: <a href=\"/"
              + name
              + "/graphql\">endpoint</a> <a href=\"/api/playground.html?schema=/"
              + name
              + "/graphql\">playground</a>");
      result.append("</li>");
    }
    result.append("</ul>");
    result.append("Version: " + Version.getVersion());
    return result.toString();
  }

  private static String openApiYaml(Request request, Response response) throws IOException {
    Schema schema = getSchema(request);
    if (schema == null) {
      throw new MolgenisException("Schema is null");
    }
    OpenAPI api = OpenApiYamlGenerator.createOpenApi(schema.getMetadata());
    response.status(200);
    return Yaml.mapper()
        .configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)
        .writeValueAsString(api);
  }

  /** get database either from session or based on token */
  // helper method used in multiple places
  public static Table getTable(Request request) {
    return getTable(request, request.params(TABLE));
  }

  /** alternative version for getTable */
  public static Table getTable(Request request, String tableName) {
    String schemaName = request.params(SCHEMA);
    Schema schema =
        sessionManager.getSession(request).getDatabase().getSchema(sanitize(schemaName));
    if (schema == null) {
      throw new MolgenisException("Schema " + schemaName + " unknown or access denied");
    } else {
      return schema.getTableById(sanitize(tableName));
    }
  }

  public static String sanitize(String string) {
    if (string != null) {
      return string.replaceAll("[\n|\r|\t]", "_");
    } else {
      return null;
    }
  }

  public static Schema getSchema(Request request) {
    if (request.params(SCHEMA) == null) {
      return null;
    }
    return sessionManager
        .getSession(request)
        .getDatabase()
        .getSchema(sanitize(request.params(SCHEMA)));
  }

  public static Collection<String> getSchemaNames(Request request) {
    return sessionManager.getSession(request).getDatabase().getSchemaNames();
  }
}
