package org.molgenis.emx2.web;

import static spark.Spark.*;

import com.google.common.io.ByteStreams;
import java.io.InputStream;
import spark.Request;
import spark.Response;
import spark.resource.ClassPathResource;
import spark.staticfiles.MimeType;

/**
 * to allow for nice urls, and make it easier for 'schema' app developers we include the schema in
 * the path without need for a router. For future this allows also permission setting to completely
 * hide a schema and disallow apps to be viewed
 *
 * <p>TODO add apps proxy service
 */
public class GroupPathMapper {

  private GroupPathMapper() {
    // hide constructor
  }

  public static void create() {
    /*
     * WARNING !! SPARK JAVA USES DESIGN WHERE THE ORDER OF REQUEST DEFINITION DETERMINES THE HANDLER
     */
    // redirect graphql api in convenient ways
    get("/:schema/graphql", GraphqlApi::handleSchemaRequests);
    post("/:schema/graphql", GraphqlApi::handleSchemaRequests);

    get("/:schema/:appname/graphql", GraphqlApi::handleSchemaRequests);
    post("/:schema/:appname/graphql", GraphqlApi::handleSchemaRequests);

    get("/:schema/:appname/theme.css", BootstrapThemeService::getCss);

    get(
        "/:schema/:app",
        (req, res) -> {
          res.redirect("/" + req.params(MolgenisWebservice.SCHEMA) + "/" + req.params("app") + "/");
          return "";
        });

    // return index.html file when in root
    get("/*/:appname/", GroupPathMapper::returnIndexFile);

    // redirect  js/css assets so they get cached between schemas (VERY GREEDY, SHOULD BE LAST CALL)
    get("/:schema/:appname/*", GroupPathMapper::redirectAssets);
  }

  private static Object returnIndexFile(Request request, Response response) {
    try {
      InputStream in =
          GroupPathMapper.class.getResourceAsStream(
              "/public_html/apps/" + request.params("appname") + "/index.html");
      return new String(ByteStreams.toByteArray(in));
    } catch (Exception e) {
      response.status(404);
      return e.getMessage();
    }
  }

  private static String redirectAssets(Request request, Response response) {
    if (!request.pathInfo().startsWith("/public_html")) {
      response.redirect(
          "/public_html/apps"
              + request.pathInfo().substring(request.params("schema").length() + 1));
      return "";
    } else {
      try {
        InputStream in = GroupPathMapper.class.getResourceAsStream(request.pathInfo());
        response.header(
            "Content-Type", MimeType.fromResource(new ClassPathResource(request.pathInfo())));
        response.raw().getOutputStream().write(ByteStreams.toByteArray(in));
        return "";
      } catch (Exception e) {
        response.status(404);
        return "File not found: " + request.pathInfo();
      }
    }
  }
}
