package org.molgenis.emx2.web;

import static org.molgenis.emx2.Constants.OIDC_CALLBACK_PATH;
import static org.molgenis.emx2.Constants.OIDC_LOGIN_PATH;
import static org.molgenis.emx2.json.JsonExceptionMapper.molgenisExceptionToJson;
import static org.molgenis.emx2.web.Constants.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;
import io.swagger.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import jakarta.servlet.DispatcherType;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlets.QoSFilter;
import org.molgenis.emx2.*;
import org.molgenis.emx2.json.JsonUtil;
import org.molgenis.emx2.utils.URIUtils;
import org.molgenis.emx2.web.controllers.OIDCController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MolgenisWebservice {
  public static final String SCHEMA = "schema";
  public static final String EDITOR = "Editor";
  public static final String MANAGER = "Manager";
  public static final String ROLE = "role";
  public static final String VIEWER = "Viewer";
  public static final long MAX_REQUEST_SIZE = 10_000_000L;
  static final String TEMPFILES_DELETE_ON_EXIT = "tempfiles-delete-on-exit";
  static final Logger logger = LoggerFactory.getLogger(MolgenisWebservice.class);
  private static final String ROBOTS_TXT = "robots.txt";
  private static final String USER_AGENT_ALLOW = "User-agent: *\nAllow: /";
  public static MolgenisSessionManager sessionManager;
  public static OIDCController oidcController;
  static URL hostUrl;

  private MolgenisWebservice() {
    // hide constructor
  }

  public static void start(int port) {

    sessionManager = new MolgenisSessionManager();
    oidcController = new OIDCController();
    Javalin app =
        Javalin.create(
                config -> {
                  config.http.maxRequestSize = MAX_REQUEST_SIZE;
                  config.router.ignoreTrailingSlashes = true;
                  config.router.treatMultipleSlashesAsSingleSlash = true;
                  config.jetty.modifyServletContextHandler(
                      handler -> {
                        handler.setSessionHandler(sessionManager.getSessionHandler());
                        FilterHolder qosFilter = new FilterHolder(new QoSFilter());
                        qosFilter.setInitParameter("maxRequests", "10");
                        handler.addFilter(qosFilter, "/*", EnumSet.of(DispatcherType.REQUEST));
                      });
                  config.jsonMapper(
                      new JavalinJackson()
                          .updateMapper(
                              mapper -> mapper.registerModule(JsonUtil.getJooqJsonModule())));
                })
            .start(port);

    try {
      hostUrl = new URL(URIUtils.extractHost(app.jettyServer().server().getURI()));
    } catch (Exception ignored) {
      // should we handle this?
    }

    MessageApi.create(app);

    app.get("/" + OIDC_CALLBACK_PATH, MolgenisWebservice::handleLoginCallback);
    app.get("/" + OIDC_LOGIN_PATH, oidcController::handleLoginRequest);
    app.get("/" + ROBOTS_TXT, MolgenisWebservice::robotsDotTxt);

    app.get(
        "/",
        ctx -> {
          // check for setting
          String landingPagePath =
              sessionManager.getSession(ctx.req()).getDatabase().getSetting(LANDING_PAGE);
          if (landingPagePath != null) {
            ctx.redirect(landingPagePath);
          } else {
            ctx.redirect("/apps/central/");
          }
        });

    app.get(
        "/api",
        ctx -> {
          ctx.contentType("text/html");
          ctx.result("Welcome to MOLGENIS EMX2 POC <br/>" + listSchemas(ctx));
        });

    // documentation operations
    app.get("/api/openapi", ctx -> ctx.result(MolgenisWebservice.listSchemas(ctx)));
    // docs per schema
    app.get("/{schema}/api/openapi", OpenApiUiFactory::getOpenApiUserInterface);
    app.get("/{schema}/api/openapi.yaml", MolgenisWebservice::openApiYaml);
    app.get(
        "/{schema}/api",
        ctx -> ctx.result("Welcome to schema api. Check <a href=\"api/openapi\">openapi</a>"));

    SiteMapService.create(app);
    CsvApi.create(app);
    ZipApi.create(app);
    ExcelApi.create(app);
    JsonApi.create(app);
    FileApi.create(app);
    JsonYamlApi.create(app);
    TaskApi.create(app);
    GraphqlApi.createGraphQLservice(app, sessionManager);
    RDFApi.create(app, sessionManager);
    GraphGenomeApi.create(app, sessionManager);
    BeaconApi.create(app, sessionManager);
    BootstrapThemeService.create(app);
    ProfilesApi.create(app);
    AnalyticsApi.create(app);

    app.get("/{schema}", MolgenisWebservice::redirectSchemaToFirstMenuItem);
    app.get("/{schema}/", MolgenisWebservice::redirectSchemaToFirstMenuItem);

    // greedy proxy stuff, always put last!
    StaticFileMapper.create(app);

    // schema members operations

    // handling of exceptions
    app.exception(
        Exception.class,
        (e, ctx) -> {
          logger.error(e.getMessage(), e);
          ctx.status(400);
          ctx.json(molgenisExceptionToJson(e));
        });
  }

  private static void handleLoginCallback(Context ctx) {
    oidcController.handleLoginCallback(ctx);
  }

  private static void robotsDotTxt(Context ctx) {
    ctx.contentType("text/plain;charset=UTF-8");
    ctx.result(USER_AGENT_ALLOW);
  }

  private static void redirectSchemaToFirstMenuItem(Context ctx) {
    try {
      Schema schema = getSchema(ctx);
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
          String location =
              "/" + ctx.pathParam(SCHEMA) + "/" + menu.get(0).get("href").replace("../", "");
          ctx.redirect(location);
        }
      } else {
        ctx.redirect("/" + ctx.pathParam(SCHEMA) + "/tables");
      }
    } catch (Exception e) {
      // silly default
      logger.debug(e.getMessage());
      ctx.redirect("/" + ctx.pathParam(SCHEMA) + "/tables");
    }
  }

  private static String listSchemas(Context ctx) {
    StringBuilder result = new StringBuilder();
    result.append("Schema independent API:");
    result.append(
        "graphql: <a href=\"/api/graphql/\">/api/graphql    </a> <a href=\"/api/playground.html?schema=/api/graphql\">playground</a>");

    result.append("<p/>Schema APIs:<ul>");
    for (String name : sessionManager.getSession(ctx.req()).getDatabase().getSchemaNames()) {
      result.append("<li>").append(name);
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

  private static String openApiYaml(Context ctx) throws IOException {
    Schema schema = getSchema(ctx);
    if (schema == null) {
      throw new MolgenisException("Schema is null");
    }
    OpenAPI api = OpenApiYamlGenerator.createOpenApi(schema.getMetadata());
    ctx.status(200);
    return Yaml.mapper()
        .configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)
        .writeValueAsString(api);
  }

  /**
   * Get the table specified in the request parameter "table".
   *
   * @param ctx the request
   * @return the table object corresponding to the table id. Never null.
   * @throws MolgenisException if the table or the schema is not found or accessible.
   */
  public static Table getTableByIdOrName(Context ctx) {
    return getTableByIdOrName(ctx, ctx.pathParam(TABLE));
  }

  /**
   * Get the table by its id.
   *
   * @param ctx the request
   * @return the table object corresponding to the table id or name. Never null.
   * @throws MolgenisException if the schema is not found or accessible.
   */
  public static Table getTableByIdOrName(Context ctx, String tableName) {
    Schema schema = getSchema(ctx);
    if (schema == null) {
      throw new MolgenisException("Schema " + ctx.pathParam(SCHEMA) + " unknown");
    }
    Table table = schema.getTableByNameOrIdCaseInsensitive(tableName);
    if (table == null) {
      throw new MolgenisException("Table " + tableName + " unknown");
    }
    return table;
  }

  public static String sanitize(String string) {
    if (string != null) {
      return string.replaceAll("[\n|\r|\t]", "_");
    } else {
      return null;
    }
  }

  public static Schema getSchema(Context ctx) {
    String schemaName = ctx.pathParamMap().get(SCHEMA);
    if (schemaName == null) {
      return null;
    }
    return sessionManager.getSession(ctx.req()).getDatabase().getSchema(sanitize(schemaName));
  }

  public static Collection<String> getSchemaNames(Context ctx) {
    return sessionManager.getSession(ctx.req()).getDatabase().getSchemaNames();
  }
}
