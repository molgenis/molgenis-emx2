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
    // return index.html file when in root
    // app.get("/*/{appname}", GroupPathMapper::returnIndexFile);

    //    // redirect  js/css assets so they get cached between schemas (VERY GREEDY, SHOULD BE LAST
    // CALL)
    //    app.get("/{schema}/{appname}/*", GroupPathMapper::redirectAssets);
  }

  private static void returnIndexFile(Context ctx) {
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

  private static void redirectAssets(Context ctx) {
    if (!ctx.path().startsWith("/public_html")) {
      ctx.redirect(
          "/public_html/apps" + ctx.path().substring(ctx.pathParam("schema").length() + 1));
    } else {

      try (InputStream in = GroupPathMapper.class.getResourceAsStream(ctx.path())) {
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
}
