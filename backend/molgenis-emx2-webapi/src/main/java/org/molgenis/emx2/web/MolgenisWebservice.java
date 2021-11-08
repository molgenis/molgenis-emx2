package org.molgenis.emx2.web;

import static org.molgenis.emx2.json.JsonExceptionMapper.molgenisExceptionToJson;
import static org.molgenis.emx2.web.Constants.*;
import static spark.Spark.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.molgenis.emx2.*;
import org.pac4j.core.client.Client;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.session.JEESessionStore;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.engine.CallbackLogic;
import org.pac4j.core.engine.DefaultCallbackLogic;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.util.FindBest;
import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.sparkjava.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Spark;

public class MolgenisWebservice {
  static final String TEMPFILES_DELETE_ON_EXIT = "tempfiles-delete-on-exit";
  static final Logger logger = LoggerFactory.getLogger(MolgenisWebservice.class);
  public static final String SCHEMA = "schema";
  static MolgenisSessionManager sessionManager;
  static String version = "undefined";

  private MolgenisWebservice() {
    // hide constructor
  }

  public static void start(int port) {

    final Config config = new SecurityConfigFactory().build();

    sessionManager = new MolgenisSessionManager();
    port(port);

    staticFiles.location("/public_html");

    get(
        "/callback",
        (request, response) -> {
          final SessionStore<SparkWebContext> bestSessionStore =
              FindBest.sessionStore(null, config, JEESessionStore.INSTANCE);
          final HttpActionAdapter<Object, SparkWebContext> bestAdapter =
              FindBest.httpActionAdapter(null, config, SparkHttpActionAdapter.INSTANCE);
          final CallbackLogic<Object, SparkWebContext> bestLogic =
              FindBest.callbackLogic(null, config, DefaultCallbackLogic.INSTANCE);

          final SparkWebContext context = new SparkWebContext(request, response, bestSessionStore);
          bestLogic.perform(context, config, bestAdapter, null, false, true, true, "MolgenisAuth");

          final ProfileManager<OidcProfile> manager = new ProfileManager<>(context);

          if (manager.get(true).isPresent()) {
            OidcProfile oidcProfile = manager.get(true).get();
            String user = oidcProfile.getEmail();
            logger.info("Signin for user: " + user);
            sessionManager.getSession(request).getDatabase().setActiveUser(user);
          }

          response.status(302);
          response.redirect("/");
          return null;
        });

    get(
        "/force-login",
        (request, response) -> {
          final SparkWebContext context = new SparkWebContext(request, response);
          final Client client =
              config.getClients().findClient(SecurityConfigFactory.OIDC_CLIENT_NAME).get();
          HttpAction action;
          try {
            action = (HttpAction) client.getRedirectionAction(context).get();
          } catch (final HttpAction e) {
            action = e;
          }
          SparkHttpActionAdapter.INSTANCE.adapt(action, context);
          return null;
        });

    // root
    get(
        "/",
        ACCEPT_HTML,
        (request, response) ->
            "Welcome to MOLGENIS EMX2 "
                + Version.getVersion()
                + ".<br/>. See <a href=\"/api/\">/api/</a> and  <a href=\"/apps/central/\">/apps/central/</a>");
    redirect.get("/", "/apps/central/");

    redirect.get("/api", "/api/");
    get(
        "/api/",
        ACCEPT_HTML,
        (request, response) ->
            "Welcome to MOLGENIS EMX2 POC <br/>" + listSchemas(request, response));

    // documentation operations
    get("/api/openapi", ACCEPT_JSON, MolgenisWebservice::listSchemas);

    // logout path, before schema because of dynamic nature of schema
    get("/logout", new LogoutRoute(new SecurityConfigFactory().build(), "/"));

    // docs per schema
    get("/:schema/api/openapi", OpenApiUiFactory::getOpenApiUserInterface);
    get("/:schema/api/openapi.yaml", MolgenisWebservice::openApiYaml);

    before("/:schema/", MolgenisWebservice::redirectSchemaToFirstMenuItem);

    get(
        "/:schema/api",
        (request, response) -> "Welcome to schema api. Check <a href=\"api/openapi\">openapi</a>");

    CsvApi.create();
    ZipApi.create();
    ExcelApi.create();
    FileApi.create();
    JsonYamlApi.create();
    TaskApi.create();
    GraphqlApi.createGraphQLservice(sessionManager);
    LinkedDataFragmentsApi.create(sessionManager);
    BootstrapThemeService.create();

    // add trailing /
    //    before(
    //        "/:schema",
    //        (req, res) -> {
    //          res.redirect("/" + req.params("schema") + "/");
    //        });
    //    before(
    //        "/:schema/:app",
    //        (req, res) -> {
    //          if (!req.params("app").equals("graphql") && !req.params("app").equals("theme.css"))
    // {
    //            res.redirect("/" + req.params("schema") + "/" + req.params("app") + "/");
    //          }
    //        });

    // greedy proxy stuff, always put last!
    GroupPathMapper.create();

    // schema members operations

    // handling of exceptions
    exception(
        Exception.class,
        (e, req, res) -> {
          logger.error(e.getMessage());
          res.status(400);
          res.type(ACCEPT_JSON);
          res.body(molgenisExceptionToJson(e));
        });
  }

  private static void redirectSchemaToFirstMenuItem(Request request, Response response) {
    try {
      Schema schema = getSchema(request);
      String role = schema.getRoleForActiveUser();
      List<Map<String, String>> menu =
          new ObjectMapper().readValue(schema.getMetadata().getSetting("menu"), List.class);
      menu =
          menu.stream()
              .filter(
                  el ->
                      role == null
                          || role.equals(schema.getDatabase().getAdminUserName())
                          || el.get("role") == null
                          || el.get("role").equals("Viewer")
                              && List.of("Viewer", "Editor", "Manager").contains(role)
                          || el.get("role").equals("Editor")
                              && List.of("Editor", "Manager").contains(role)
                          || el.get("role").equals("Manager") && role.equals("Manager"))
              .collect(Collectors.toList());

      if (menu.size() > 0) {
        response.redirect(
            "/" + request.params("schema") + "/" + menu.get(0).get("href").replace("../", ""));
      }
    } catch (Exception e) {
      // silly default
      logger.debug(e.getMessage());
    }
    response.redirect("/" + request.params("schema") + "/tables");
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
    String schemaName = request.params(SCHEMA);
    Schema schema =
        sessionManager.getSession(request).getDatabase().getSchema(sanitize(schemaName));
    if (schema == null) {
      throw new MolgenisException("Schema " + schemaName + " unknown or access denied");
    }
    return schema.getTable(sanitize(request.params(TABLE)));
  }

  public static Schema getSchema(Request request) {
    return sessionManager
        .getSession(request)
        .getDatabase()
        .getSchema(sanitize(request.params(SCHEMA)));
  }
}
