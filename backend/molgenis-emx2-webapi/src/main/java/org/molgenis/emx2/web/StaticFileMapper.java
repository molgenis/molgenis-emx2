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
    app.get("*/{app}/<asset>", ServeStaticFile::serve);
    app.get("*/{app}", ServeStaticFile::serve);
  }

  private static void redirectDocs(Context ctx) {
    ServeStaticFile.serve(ctx, "/public_html/apps/docs/" + ctx.pathParam("asset"));
  }
}
