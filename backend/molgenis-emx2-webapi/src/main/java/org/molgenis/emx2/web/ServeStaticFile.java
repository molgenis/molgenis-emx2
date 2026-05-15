package org.molgenis.emx2.web;

import static org.molgenis.emx2.ColumnType.STRING;

import com.google.common.io.ByteStreams;
import io.javalin.http.Context;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
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

  private record AppsPath(boolean isFile, boolean isUi, String mimeType, String path) {

    public static AppsPath fromContext(Context ctx) {
      // Dissect the path, so we can check for the folder to serve from
      // Strip context path prefix so static file resolution is context-path-agnostic
      String rawPath = ctx.path();
      String contextPath = Objects.requireNonNullElse(ctx.contextPath(), "");
      if (!contextPath.isEmpty() && rawPath.startsWith(contextPath)) {
        rawPath = rawPath.substring(contextPath.length());
      }
      String[] segments = rawPath.split("/");
      List<String> parts = new ArrayList<>();

      for (String segment : segments) {
        if (!segment.isEmpty()) {
          parts.add(segment);
        }
      }

      boolean isFile = parts.getLast().contains(("."));
      boolean isUi = !isFile && parts.size() > 1 && Objects.equals(parts.get(1), "ui");

      // check if the path starts with apps, if not, remove the first bit <schema>
      if (!Objects.equals(parts.getFirst(), "apps")) {
        parts.set(0, "apps");
      }
      String path = String.join("/", parts);
      String mimeType = URLConnection.guessContentTypeFromName(path);

      return new AppsPath(isFile, isUi, mimeType, path);
    }
  }

  private ServeStaticFile() {
    // hide constructor
  }

  private static final String CUSTOM_APP_FOLDER = "custom-app/";
  private static final String INTERNAL_APP_FOLDER = "/public_html/";
  private static final String NOT_FOUND = "File not found: ";

  public static final String CUSTOM_APP_PATH =
      (String) EnvironmentProperty.getParameter(Constants.CUSTOM_APP_PATH, "", STRING);

  private static String convertWindowsPathToJarPath(String potentialFile) {
    return potentialFile.replace("\\", "/");
  }

  private static Path getJarDirectory() {

    try {
      Path jarPath =
          Paths.get(
              ServeStaticFile.class.getProtectionDomain().getCodeSource().getLocation().toURI());

      /* Check if we are in the main jar and not in web-api like in IDE */
      if (Files.isRegularFile(jarPath) && !jarPath.toString().contains("webapi")) {
        return jarPath.getParent();
      } else {
        // Running from IDE/CLI (classes folder)
        Path emx2Home = jarPath.getParent();
        do {
          emx2Home = emx2Home.getParent();
        } while (!emx2Home.toString().endsWith("backend"));
        // One more above backend is root
        return emx2Home.getParent();
      }
    } catch (Exception e) {
      throw new MolgenisException("Cannot determine JAR location", e);
    }
  }

  private static boolean tryServeJarApp(Context ctx, String potentialFile, String mimeType) {
    String potentialJarFile = convertWindowsPathToJarPath(potentialFile);

    try (InputStream in = ServeStaticFile.class.getResourceAsStream(potentialJarFile)) {
      // App found inside the jar!
      if (in != null) {
        send(ctx, in, mimeType);
        return true;
      }
    } catch (Exception internalNotFound) {
      // Catch silently
    }
    return false;
  }

  private static boolean tryServeExternalApp(Context ctx, String potentialFile, String mimeType) {

    try (InputStream in = new FileInputStream(potentialFile)) {
      send(ctx, in, mimeType);
      return true;

    } catch (Exception internalNotFound) {
      // catch silently
    }
    return false;
  }

  // Serve internal file
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
    AppsPath appsPath = AppsPath.fromContext(ctx);

    String path = appsPath.path;
    String fallbackFileBase = path + "/index.html";

    if (appsPath.isUi) {
      fallbackFileBase = "apps/ui/index.html";
    }

    Path internalAppsDirectory = Paths.get(INTERNAL_APP_FOLDER);
    String requestedInternalFilePath = internalAppsDirectory.resolve(path).normalize().toString();
    String internalFallbackFile =
        internalAppsDirectory.resolve(fallbackFileBase).normalize().toString();

    if (!requestedInternalFilePath.startsWith(internalAppsDirectory.toString())) {
      ctx.status(403).result("Forbidden");
      return;
    }

    if (appsPath.isFile) {
      boolean internalRequestFound =
          tryServeJarApp(ctx, requestedInternalFilePath, appsPath.mimeType);
      if (internalRequestFound) return;
    }

    boolean internalFallbackFound = tryServeJarApp(ctx, internalFallbackFile, "text/html");
    if (internalFallbackFound) return;

    // External can not use apps
    path = path.replace("apps/", "");

    // Nothing found, so now check external app
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
      // Suspected path traversal: reject the request
      ctx.status(403).result("Forbidden");
      return;
    }

    if (appsPath.isFile) {
      boolean externalRequestFound =
          tryServeExternalApp(ctx, requestedExternalFilePath.toString(), appsPath.mimeType);
      if (externalRequestFound) return;
    }

    boolean externalFallbackFound = tryServeExternalApp(ctx, externalFallbackFile, "text/html");
    if (externalFallbackFound) return;

    // Tried out best to serve something, sadly nothing was found!
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
      byte[] bytes = ByteStreams.toByteArray(in);
      ctx.header("Content-Type", mimeType);

      if ("text/html".equals(mimeType)) {
        String contextPath = Objects.requireNonNullElse(ctx.contextPath(), "");
        String safeContextPath = contextPath.replaceAll("[^a-zA-Z0-9/_-]", "");
        String injection =
            "<script>window.__molgenisContextPath='" + safeContextPath + "';</script>";
        String html = new String(bytes, StandardCharsets.UTF_8);
        int headClose = html.indexOf("</head>");
        if (headClose >= 0) {
          html = html.substring(0, headClose) + injection + html.substring(headClose);
        } else {
          html = injection + html;
        }
        ctx.result(html.getBytes(StandardCharsets.UTF_8));
      } else {
        ctx.result(bytes);
      }
    } catch (Exception e) {
      ctx.status(404).result(NOT_FOUND + ctx.path());
    }
  }
}
