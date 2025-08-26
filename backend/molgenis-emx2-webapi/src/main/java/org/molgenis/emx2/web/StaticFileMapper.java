package org.molgenis.emx2.web;

import com.google.common.io.ByteStreams;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.MolgenisException;

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
        "/apps/ui/{schema}/{path}",
        ctx -> {
          if (ctx.pathParam("path").contains(".") || ctx.pathParam("path").contains("/")) {
            redirectResources(ctx);
          } else {
            returnUiAppIndex(ctx);
          }
        });

    app.get("*/{app}/assets/<asset>", StaticFileMapper::redirectAssets);
    app.get("*/{app}/img/<asset>", StaticFileMapper::redirectImg);
    app.get("/apps/{app}/<asset>", StaticFileMapper::redirectResources);

    app.get("*/{app}/index.html", StaticFileMapper::returnIndexFile);
    app.get("*/{app}", StaticFileMapper::returnIndexFile);
  }

  private static void redirectDirectory(Context ctx) {
    String path = "/public_html/apps/directory/" + ctx.pathParam("asset");
    addFileToContext(ctx, path, null);
  }

  private static void redirectImg(Context ctx) {
    String path = "/public_html/apps/" + ctx.pathParam("app") + "/img/" + ctx.pathParam("asset");
    addFileToContext(ctx, path, null);
  }

  private static void redirectResources(Context ctx) {
    addFileToContext(ctx, "/public_html" + ctx.path(), null);
  }

  private static void redirectDocs(Context ctx) {
    addFileToContext(ctx, "/public_html/apps/docs/" + ctx.pathParam("asset"), null);
  }

  private static void redirectAssets(Context ctx) {
    String path = "/public_html/apps/" + ctx.pathParam("app") + "/assets/" + ctx.pathParam("asset");
    addFileToContext(ctx, path, null);
  }

  private static void returnIndexFile(Context ctx) {
    // redirect apps from /schema/app to /schema/app
    if (!ctx.path().endsWith("/")) {
      // validate valid app path
      List<String> path =
          Arrays.stream(ctx.path().split("/")).filter(item -> item.length() > 0).toList();
      if (path.size() != 2) {
        throw new MolgenisException("invalid app path. Needs to be /schema/app");
      }
      // validate that it is a valid schema name
      if (!path.get(0).matches(Constants.SCHEMA_NAME_REGEX)) {
        throw new MolgenisException(
            "path invalid: schema name '" + ctx.pathParam("schema") + "' is invalid.");
      }
      // validate that it is a known app
      URL app = StaticFileMapper.class.getResource("/public_html/apps/" + ctx.pathParam("app"));
      if (app == null) {
        throw new MolgenisException("path invalid: app '" + ctx.pathParam("app") + "' is unknown.");
      }
      ctx.redirect("/" + path.get(0) + "/" + path.get(1) + "/");
    }
    String path = "/public_html/apps/" + ctx.pathParam("app") + "/index.html";
    addFileToContext(ctx, path, "text/html");
  }

  private static void returnUiAppIndex(Context ctx) {
    addFileToContext(ctx, "/public_html/apps/ui/index.html", "text/html");
  }

  public static void addFileToContext(Context ctx, String path, String mimeType) {
    try (InputStream in = StaticFileMapper.class.getResourceAsStream(path)) {
      if (in == null) {
        ctx.status(404).result("File not found: " + ctx.path());
        return;
      }
      if (mimeType == null) {
        mimeType = URLConnection.guessContentTypeFromName(path);

        if (mimeType == null) {
          mimeType = Files.probeContentType(Path.of(path));
        }

        if (mimeType == null) {
          mimeType = "application/octet-stream";
        }
      }
      ctx.contentType(mimeType);
      ctx.result(ByteStreams.toByteArray(in));
    } catch (Exception e) {
      ctx.status(404).result("File not found: " + ctx.path());
    }
  }
}
