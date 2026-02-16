package org.molgenis.emx2.web;

import com.google.common.io.ByteStreams;
import io.javalin.http.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServeWebFile {

  /* Serve internal file */
  public static void Serve(Context ctx, String path) {

    try (InputStream in = StaticFileMapper.class.getResourceAsStream(path)) {
      String mimeType = URLConnection.guessContentTypeFromName(path);

      if (mimeType == null) {
        mimeType = Files.probeContentType(Path.of(path));
      }
      send(ctx, in, mimeType);
    } catch (Exception e) {
      ctx.status(404).result("File not found: " + ctx.path());
    }
  }

  /* When only ctx is given, serve external file */
  public static void Serve(Context ctx) {
    String path = System.getProperty("user.dir") + "/custom-app/" + ctx.pathParam("path...");

    File file = new File(path);
    /* Check if it's a file, and has an extension, if it is neither, then we fall back to index.html for SPA */
    if ((!file.exists() || !file.isFile()) || !path.matches(".*\\.[A-Za-z0-9]{1,4}$")) {
      path += "/index.html"; /* So we can determine mimetype automatically */
      file = new File(path);
    }

    try (InputStream in = new FileInputStream(file)) {
      String mimeType = URLConnection.guessContentTypeFromName(path);

      if (mimeType == null) {
        mimeType = Files.probeContentType(Path.of(path));
      }

      send(ctx, in, mimeType);
    } catch (Exception e) {
      ctx.status(404).result("File not found: " + ctx.path());
    }
  }

  private static void send(Context ctx, InputStream in, String mimeType) {
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
