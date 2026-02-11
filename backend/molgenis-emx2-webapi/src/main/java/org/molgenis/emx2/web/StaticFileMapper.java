package org.molgenis.emx2.web;

import com.google.common.io.ByteStreams;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

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

    /* Make the app paths explicit, do not merge them in the same way as schema browser */
    app.get("/apps/{app}/assets/<asset>", StaticFileMapper::redirectAssets);
    app.get("/apps/{app}/img/<asset>", StaticFileMapper::redirectImg);
    app.get("/apps/{app}/<asset>", StaticFileMapper::redirectResources);
    app.get("/apps/{app}/index.html", StaticFileMapper::returnIndexFile);
    app.get("/apps/{app}", StaticFileMapper::returnIndexFile);

    /* Serve a custom app in a folder next to the jar */
    app.get("/ext/{path...}", StaticFileMapper::addExternalFileToContext);

    /* These are for all the user made schemas / tableview in bootstrap. */
    app.get("*/{app}/assets/<asset>", StaticFileMapper::redirectAssets);
    app.get("*/{app}/img/<asset>", StaticFileMapper::redirectImg);
    app.get("*/{app}/<asset>", StaticFileMapper::redirectResources);
    app.get("*/{app}/index.html", StaticFileMapper::returnIndexFile);
    app.get("*/{app}", StaticFileMapper::returnIndexFile);
  }

  private static void redirectDirectory(Context ctx) {
    String path = "/public_html/apps/directory/" + ctx.pathParam("asset");
    addInternalFileToContext(ctx, path, null);
  }

  private static void redirectImg(Context ctx) {
    String path = "/public_html/apps/" + ctx.pathParam("app") + "/img/" + ctx.pathParam("asset");
    addInternalFileToContext(ctx, path, null);
  }

  private static void redirectResources(Context ctx) {
    addInternalFileToContext(ctx, "/public_html" + ctx.path(), null);
  }

  private static void redirectDocs(Context ctx) {
    addInternalFileToContext(ctx, "/public_html/apps/docs/" + ctx.pathParam("asset"), null);
  }

  private static void redirectAssets(Context ctx) {
    String path = "/public_html/apps/" + ctx.pathParam("app") + "/assets/" + ctx.pathParam("asset");
    addInternalFileToContext(ctx, path, null);
  }

  private static void returnIndexFile(Context ctx) {
    String path = "/public_html/apps/" + ctx.pathParam("app") + "/index.html";
    addInternalFileToContext(ctx, path, "text/html");
  }

  private static void returnUiAppIndex(Context ctx) {
    addInternalFileToContext(ctx, "/public_html/apps/ui/index.html", "text/html");
  }

  public static void addInternalFileToContext(Context ctx, String path, String mimeType) {
    try (InputStream in = StaticFileMapper.class.getResourceAsStream(path)) {

      if (mimeType == null) {
        mimeType = URLConnection.guessContentTypeFromName(path);

        if (mimeType == null) {
          mimeType = Files.probeContentType(Path.of(path));
        }
      }
      addFileToContext(ctx, in, mimeType);
    } catch (Exception e) {
      ctx.status(404).result("File not found: " + ctx.path());
    }
  }

  public static void addExternalFileToContext(Context ctx) {
    String path = System.getProperty("user.dir") + "/custom-app/" + ctx.pathParam("path...");
    String mimeType;

    File file = new File(path);
    /* Check if it's a file, and has an extension, if it is neither, then we fall back to index.html for SPA */
    if ((!file.exists() || !file.isFile()) || !path.matches(".*\\.[A-Za-z0-9]{1,4}$")) {
      path += "/index.html"; /* So we can determine mimetype automatically */
      file = new File(path);
    }

    try (InputStream in = new FileInputStream(file)) {
      mimeType = URLConnection.guessContentTypeFromName(path);

      if (mimeType == null) {
        mimeType = Files.probeContentType(Path.of(path));
      }

      addFileToContext(ctx, in, mimeType);
    } catch (Exception e) {
      ctx.status(404).result("File not found: " + ctx.path());
    }
  }

  private static void addFileToContext(Context ctx, InputStream in, String mimeType) {
    if (in == null) {
      ctx.status(404).result("File not found: " + ctx.path());
      return;
    }

    if (mimeType == null) {
      mimeType = "application/octet-stream";
    }

    try {
      ctx.contentType(mimeType);

      ctx.result(ByteStreams.toByteArray(in));
    } catch (Exception e) {
      ctx.status(404).result("File not found: " + ctx.path());
    }
  }
}
