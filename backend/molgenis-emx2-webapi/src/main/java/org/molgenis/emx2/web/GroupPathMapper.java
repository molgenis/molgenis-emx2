package org.molgenis.emx2.web;

import com.google.common.io.ByteStreams;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

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

  public static void create(Javalin app) {

    app.get("/{schema}/{appname}/theme.css", BootstrapThemeService::getCss);

    app.get("*/docs/<asset>", GroupPathMapper::redirectDocs);

    app.get("*/{app}/assets/<asset>", GroupPathMapper::redirectAssets);
    app.get("/apps/resources/<asset>", GroupPathMapper::redirectResources);
    app.get("/apps/{app}/<asset>", GroupPathMapper::redirectResources);

    app.get("*/{appname}/index.html", GroupPathMapper::returnIndexFile);
    app.get("*/{appname}", GroupPathMapper::returnIndexFile);
  }

  private static void redirectResources(Context ctx) {
    getFileFromClasspath(ctx, "/public_html" + ctx.path());
  }

  private static void redirectDocs(Context ctx) {
    getFileFromClasspath(ctx, "/public_html/apps/docs/" + ctx.pathParam("asset"));
  }

  private static void redirectAssets(Context ctx) {
    String path = "/public_html/apps/" + ctx.pathParam("app") + "/assets/" + ctx.pathParam("asset");
    getFileFromClasspath(ctx, path);
  }

  private static void returnIndexFile(Context ctx) {
    if (!ctx.path().endsWith("/")) ctx.redirect(ctx.path() + "/");
    try {
      InputStream in =
          GroupPathMapper.class.getResourceAsStream(
              "/public_html/apps/" + ctx.pathParam("appname") + "/index.html");

      if (in == null) {
        ctx.status(404).result("File not found");
      } else {
        String fileContent = new String(ByteStreams.toByteArray(in), StandardCharsets.UTF_8);
        ctx.contentType("text/html").result(fileContent);
      }
    } catch (Exception e) {
      ctx.status(404).result("Error: " + e.getMessage());
    }
  }

  private static void getFileFromClasspath(Context ctx, String path) {
    try (InputStream in = GroupPathMapper.class.getResourceAsStream(path)) {
      if (in == null) {
        ctx.status(404).result("File not found: " + ctx.path());
        return;
      }
      String mimeType = URLConnection.guessContentTypeFromName(ctx.path());
      if (mimeType == null) {
        mimeType = "application/octet-stream";
      }
      ctx.contentType(mimeType);
      ctx.result(ByteStreams.toByteArray(in));
    } catch (Exception e) {
      ctx.status(404).result("File not found: " + ctx.path());
    }
  }
}
