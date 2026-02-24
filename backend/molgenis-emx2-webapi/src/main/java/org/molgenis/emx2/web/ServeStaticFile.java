package org.molgenis.emx2.web;

import static org.molgenis.emx2.ColumnType.STRING;

import com.google.common.io.ByteStreams;
import io.javalin.http.Context;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.utils.EnvironmentProperty;

public class ServeStaticFile {

  private static final String CUSTOM_APP_FOLDER = "custom-app/";
  private static final String INTERNAL_APP_FOLDER = "/public_html/";
  private static final String NOT_FOUND = "File not found: ";

  public static final String CUSTOM_APP_PATH =
      (String) EnvironmentProperty.getParameter(Constants.CUSTOM_APP_PATH, "", STRING);

  private static Path getJarDirectory() {

    try {
      Path jarPath =
          Paths.get(
              ServeStaticFile.class.getProtectionDomain().getCodeSource().getLocation().toURI());

      /* Check if we are in the main jar and not in web-api like in IDE */
      if (Files.isRegularFile(jarPath) && !jarPath.toString().contains("webapi")) {
        return jarPath.getParent();
      } else {
        /* Running from IDE/CLI (classes folder) */
        Path emx2Home = jarPath.getParent();
        do {
          emx2Home = emx2Home.getParent();
        } while (!emx2Home.toString().endsWith("backend"));
        /* One more above backend is root */
        return emx2Home.getParent();
      }
    } catch (Exception e) {
      throw new MolgenisException("Cannot determine JAR location", e);
    }
  }

  private static boolean TryServeJarApp(Context ctx, String potentialFile) {
    String mimeType = URLConnection.guessContentTypeFromName(potentialFile);

    try (InputStream in = ServeStaticFile.class.getResourceAsStream(potentialFile)) {
      if (mimeType == null) {
        mimeType = Files.probeContentType(Path.of(potentialFile));
      }
      /* App found inside the jar! */
      if (in != null) {
        send(ctx, in, mimeType);
        return true;
      }
    } catch (Exception internalNotFound) {
      /* Catch silently */
    }
    return false;
  }

  private static boolean TryServeExternalApp(Context ctx, String potentialFile) {
    String mimeType = URLConnection.guessContentTypeFromName(potentialFile);

    try (InputStream in = new FileInputStream(potentialFile)) {
      if (mimeType == null) {
        mimeType = Files.probeContentType(Path.of(potentialFile));
      }
      send(ctx, in, mimeType);
      return true;

    } catch (Exception internalNotFound) {
      /* catch silently */
    }
    return false;
  }

  /* Serve internal file */
  public static void serve(Context ctx, String path) {

    try (InputStream in = ServeStaticFile.class.getResourceAsStream(path)) {
      String mimeType = URLConnection.guessContentTypeFromName(path);

      if (mimeType == null) {
        mimeType = Files.probeContentType(Path.of(path));
      }
      send(ctx, in, mimeType);
    } catch (Exception e) {
      ctx.status(404).result(NOT_FOUND + ctx.path());
    }
  }

  public static void serve(Context ctx) {
    String path = ctx.path();
    /* Dissect the path, so we can check for the folder to serve from */
    String[] segments = path.split("/");

    List<String> parts = new ArrayList<>();
    for (String segment : segments) {
      if (!segment.isEmpty()) {
        parts.add(segment);
      }
    }

    /* check if the path starts with apps, if not, remove the first bit <schema> */
    if (!Objects.equals(parts.getFirst(), "apps")) {
      parts.set(0, "apps");
    }

    /* Remove leading /, so that it will resolve correctly */
    path = String.join("/", parts);
    boolean isFile = parts.getLast().contains(("."));
    String fallbackFileBase = path + "/index.html";

    if (Objects.equals(parts.get(1), "ui") && !isFile) {
      fallbackFileBase = "apps/ui/index.html";
    }

    Path internalAppsDirectory = Paths.get(INTERNAL_APP_FOLDER);
    String requestedInternalFilePath =
        internalAppsDirectory.resolve(path).toAbsolutePath().normalize().toString();
    String internalFallbackFile =
        internalAppsDirectory.resolve(fallbackFileBase).toAbsolutePath().normalize().toString();

    if (isFile) {
      boolean internalRequestFound = TryServeJarApp(ctx, requestedInternalFilePath);
      if (internalRequestFound) return;
    }

    boolean internalFallbackFound = TryServeJarApp(ctx, internalFallbackFile);
    if (internalFallbackFound) return;

    /* External can not use apps */
    path = path.replace("apps/", "");

    /* Nothing found, so now check external app */
    Path externalAppsDirectory =
        Paths.get(
                Objects.requireNonNullElseGet(
                    CUSTOM_APP_PATH, () -> getJarDirectory() + "/" + CUSTOM_APP_FOLDER))
            .toAbsolutePath()
            .normalize();

    Path requestedExternalFilePath =
        externalAppsDirectory.resolve(path).toAbsolutePath().normalize();
    String externalFallbackFile =
        externalAppsDirectory.resolve(path + "/index.html").toAbsolutePath().normalize().toString();

    if (!requestedExternalFilePath.startsWith(externalAppsDirectory)) {
      /* Suspected path traversal: reject the request */
      ctx.status(403).result("Forbidden");
      return;
    }

    if (isFile) {
      boolean externalRequestFound = TryServeExternalApp(ctx, requestedExternalFilePath.toString());
      if (externalRequestFound) return;
    }

    boolean externalFallbackFound = TryServeExternalApp(ctx, externalFallbackFile);
    if (externalFallbackFound) return;

    /* Tried out best to serve something, sadly nothing was found! */
    ctx.status(404).result(NOT_FOUND + ctx.path());
  }

  private static void send(Context ctx, InputStream in, String mimeType) {
    if (in == null) {
      ctx.status(404).result(NOT_FOUND + ctx.path());
      return;
    }

    if (mimeType == null) {
      mimeType = "application/octet-stream";
    }

    try {
      ctx.contentType(mimeType);
      ctx.result(ByteStreams.toByteArray(in));
    } catch (Exception e) {
      ctx.status(404).result(NOT_FOUND + ctx.path());
    }
  }
}
