package org.molgenis.emx2.web;

import com.google.common.io.ByteStreams;
import org.molgenis.emx2.web.graphql.GraphqlApi;
import spark.Request;
import spark.Response;

import java.io.InputStream;

import static spark.Spark.get;
import static spark.Spark.post;

/**
 * to allow for nice urls, and make it easier for 'schema' app developers we include the schema in
 * the path without need for a router. For future this allows also permission setting to completely
 * hide a schema and disallow apps to be viewed
 */
public class GroupPathMapper {

  private GroupPathMapper() {
    // hide constructor
  }

  public static void create() {

    // redirect graphql api in convenient ways
    get("/:schema/graphql", GraphqlApi::handleSchemaRequests);
    post("/:schema/graphql", GraphqlApi::handleSchemaRequests);

    get("/:schema/:appname/graphql", GraphqlApi::handleSchemaRequests);
    post("/:schema/:appname/graphql", GraphqlApi::handleSchemaRequests);

    // return index.html file when in root
    get("/*/:appname/", GroupPathMapper::returnIndexFile);

    // redirect  js/css assets so they get cached between schemas
    get("/*/:appname/*", GroupPathMapper::redirectAssets);
    //   }
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
    String path =
        request
            .pathInfo()
            .substring(request.pathInfo().indexOf("/" + request.params("appname") + "/"));
    response.redirect("/apps" + path);
    return "";
  }
}
