package org.molgenis.emx2.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.utils.URLUtils.extractBaseURL;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.testtools.JavalinTest;
import jakarta.servlet.ServletOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Original test used `app.start(port)` instead to actually test when a port is set if the behaviour
 * was correct or not, but simplified the tests as Azure failed to build due to no permission to use
 * port 80. Current tests only validate by setting `config.contextResolver.host`.
 */
class URLUtilsTest {
  private void runTestConfig(Handler handler, String expected, String host, String contextPath) {
    Javalin app =
        Javalin.create(
            config -> {
              config.router.ignoreTrailingSlashes = true;
              config.router.treatMultipleSlashesAsSingleSlash = true;
              if (host != null) config.contextResolver.host = ctx -> host;
              if (contextPath != null) config.router.contextPath = contextPath;
            });
    app.get("/test", handler);

    JavalinTest.test(
        app,
        ((server, client) -> {
          if (contextPath != null) {
            assertEquals(expected, client.get(contextPath + "/test").body().string());
          } else {
            assertEquals(expected, client.get("/test").body().string());
          }
        }));
  }

  private void contextWrapper(Context ctx, byte[] bytes) {
    ServletOutputStream outputStream = ctx.outputStream();
    try {
      outputStream.write(bytes);
      outputStream.flush();
      outputStream.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void testBaseUrlMolgenis() {
    runTestConfig(
        (ctx) -> contextWrapper(ctx, extractBaseURL(ctx).getBytes()),
        "http://molgenis.org/",
        "molgenis.org",
        null);
  }

  @Test
  void testBaseUrlMolgenis80() {
    runTestConfig(
        (ctx) -> contextWrapper(ctx, extractBaseURL(ctx).getBytes()),
        "http://molgenis.org/",
        "molgenis.org:80",
        null);
  }

  @Test
  void testBaseUrlMolgenis8080() {
    runTestConfig(
        (ctx) -> contextWrapper(ctx, extractBaseURL(ctx).getBytes()),
        "http://molgenis.org:8080/",
        "molgenis.org:8080",
        null);
  }

  /** Most complex scenario. */
  @Test
  void testBaseUrlMolgenisSubdir8080() {
    runTestConfig(
        (ctx) -> contextWrapper(ctx, extractBaseURL(ctx).getBytes()),
        "http://molgenis.org:8080/subdir/",
        "molgenis.org:8080",
        "/subdir");
  }

  @Test
  void testContextPathTrailingSlash() {
    runTestConfig(
        (ctx) -> contextWrapper(ctx, extractBaseURL(ctx).getBytes()),
        "http://molgenis.org:8080/subdir/",
        "molgenis.org:8080",
        "/subdir/");
  }

  @Test
  void testDefaultPort() {
    assertAll(
        () -> assertTrue(URLUtils.isDefaultPort("http", "80")),
        () -> assertFalse(URLUtils.isDefaultPort("http", "8080")),
        () -> assertTrue(URLUtils.isDefaultPort("https", "443")),
        () -> assertFalse(URLUtils.isDefaultPort("https", "80")));
  }
}
