package org.molgenis.emx2.web;

import io.javalin.Javalin;
import io.javalin.http.Context;

/**
 * to allow for nice urls, and make it easier for 'schema' app developers we include the schema in
 * the path without need for a router. For future this allows also permission setting to completely
 * hide a schema and disallow apps to be viewed
 *
 * <p>TODO add apps proxy service
 */
public class StaticFileMapper {

  private StaticFileMapper() {
    // hide constructor
  }

  public static void create(Javalin app) {

    app.get("/{schema}/{appname}/theme.css", BootstrapThemeService::getCss);

    app.get("*/docs/<asset>", StaticFileMapper::redirectDocs);
    app.get("/{schema}/directory/<asset>", StaticFileMapper::redirectDirectory);

    app.get("/apps/ui/{schema}/", StaticFileMapper::returnUiAppIndex);
    app.get(
        "/apps/ui/{schema}/<path>",
        ctx -> {
          if (ctx.pathParam("path").contains(".")) {
            redirectResources(ctx);
          } else {
            returnUiAppIndex(ctx);
          }
        });

    /* Serve a custom app in a folder next to the jar */
    app.get("/ext/{app}", ServeStaticFile::serve);
    app.get("/ext/{app}/<asset>", ServeStaticFile::serve);

    /* These are for all the user made schemas / tableview in bootstrap and internal apps. */
    app.get("*/{app}/assets/<asset>", StaticFileMapper::redirectAssets);
    app.get("*/{app}/img/<asset>", StaticFileMapper::redirectImg);
    app.get("/apps/{app}/<asset>", StaticFileMapper::redirectResources);

    app.get("*/{app}/index.html", StaticFileMapper::returnIndexFile);
    app.get("*/{app}", StaticFileMapper::returnIndexFile);
  }

  private static void redirectDirectory(Context ctx) {
    String path = "/public_html/apps/directory/" + ctx.pathParam("asset");
    ServeStaticFile.serve(ctx, path);
  }

  private static void redirectImg(Context ctx) {
    String path = "/public_html/apps/" + ctx.pathParam("app") + "/img/" + ctx.pathParam("asset");
    ServeStaticFile.serve(ctx, path);
  }

  private static void redirectResources(Context ctx) {
    ServeStaticFile.serve(ctx, "/public_html" + ctx.path());
  }

  private static void redirectDocs(Context ctx) {
    ServeStaticFile.serve(ctx, "/public_html/apps/docs/" + ctx.pathParam("asset"));
  }

  private static void redirectAssets(Context ctx) {
    String path = "/public_html/apps/" + ctx.pathParam("app") + "/assets/" + ctx.pathParam("asset");
    ServeStaticFile.serve(ctx, path);
  }

  private static void returnIndexFile(Context ctx) {
    String path = "/public_html/apps/" + ctx.pathParam("app") + "/index.html";
    ServeStaticFile.serve(ctx, path);
  }

  private static void returnUiAppIndex(Context ctx) {
    ServeStaticFile.serve(ctx, "/public_html/apps/ui/index.html");
  }
}
