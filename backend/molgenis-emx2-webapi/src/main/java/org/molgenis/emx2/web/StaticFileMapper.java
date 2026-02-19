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
    app.get("/ext/{app}/<asset>", ServeStaticFile::serve);
    app.get("/ext/{app}", ServeStaticFile::serve);
    app.get("*/{app}/<asset>", StaticFileMapper::redirectInternal);
    app.get("*/{app}", StaticFileMapper::returnIndexFile);
  }

  private static void redirectInternal(Context ctx) {
    String ctxPath = ctx.path();
    /* just to be sure */
    if (!ctxPath.contains(("apps/"))) {
      ctxPath = "/apps" + ctxPath;
    }
    ServeStaticFile.serve(ctx, "/public_html" + ctxPath);
  }

  private static void redirectResources(Context ctx) {
    ServeStaticFile.serve(ctx, "/public_html" + ctx.path());
  }

  private static void redirectDocs(Context ctx) {
    ServeStaticFile.serve(ctx, "/public_html/apps/docs/" + ctx.pathParam("asset"));
  }

  private static void returnIndexFile(Context ctx) {
    String path = "/public_html/apps/" + ctx.pathParam("app") + "/index.html";
    ServeStaticFile.serve(ctx, path);
  }

  private static void returnUiAppIndex(Context ctx) {
    ServeStaticFile.serve(ctx, "/public_html/apps/ui/index.html");
  }
}
