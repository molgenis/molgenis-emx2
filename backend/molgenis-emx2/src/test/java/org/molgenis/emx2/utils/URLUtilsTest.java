package org.molgenis.emx2.utils;

import static org.junit.jupiter.api.Assertions.*;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.testtools.DefaultTestConfig;
import io.javalin.testtools.HttpClient;
import jakarta.servlet.ServletOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

// Isolated to ensure ports are free and not used by other tests.
// ExecutionMode.SAME_THREAD to ensure tests using same port aren't run simultaneously.
@Isolated
@Execution(ExecutionMode.SAME_THREAD)
class URLUtilsTest {
  private void runTestConfig(
      Handler handler, String expected, String host, int port, String contextPath) {
    Javalin app =
        Javalin.create(
            config -> {
              config.router.ignoreTrailingSlashes = true;
              config.router.treatMultipleSlashesAsSingleSlash = true;
              if (host != null) config.contextResolver.host = ctx -> host;
              if (contextPath != null) config.router.contextPath = contextPath;
            });
    app.get("/test", handler);

    // ctx.port() through JavalinTest.test() changes everytime and cannot be set with app.start()
    try {
      app.start(port);
      HttpClient client = new HttpClient(app, DefaultTestConfig.getOkHttpClient());
      if (contextPath != null) {
        assertEquals(expected, client.get(contextPath + "/test").body().string());
      } else {
        assertEquals(expected, client.get("/test").body().string());
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      app.stop();
    }
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
  void testBaseUrlLocalHost() {
    runTestConfig(
        (ctx) -> contextWrapper(ctx, URLUtils.extractBaseURL(ctx).getBytes()),
        "http://127.0.0.1/",
        null,
        80,
        null);
  }

  @Test
  void testBaseUrlLocalHost80() {
    runTestConfig(
        (ctx) -> contextWrapper(ctx, URLUtils.extractBaseURL(ctx).getBytes()),
        "http://127.0.0.1/",
        "127.0.0.1:80",
        80,
        null);
  }

  @Test
  void testBaseUrlLocalHost8080() {
    runTestConfig(
        (ctx) -> contextWrapper(ctx, URLUtils.extractBaseURL(ctx).getBytes()),
        "http://127.0.0.1:8080/",
        null,
        8080,
        null);
  }

  @Test
  void testBaseUrlMolgenis() {
    runTestConfig(
        (ctx) -> contextWrapper(ctx, URLUtils.extractBaseURL(ctx).getBytes()),
        "http://molgenis.org/",
        "molgenis.org",
        80,
        "");
  }
  
  @Test
  void testBaseUrlMolgenis80() {
    runTestConfig(
        (ctx) -> contextWrapper(ctx, URLUtils.extractBaseURL(ctx).getBytes()),
        "http://molgenis.org/",
        "molgenis.org:80",
        80,
        "");
  }

  @Test
  void testBaseUrlMolgenis8080() {
    runTestConfig(
        (ctx) -> contextWrapper(ctx, URLUtils.extractBaseURL(ctx).getBytes()),
        "http://molgenis.org:8080/",
        "molgenis.org:8080",
        8080,
        "");
  }

  /** Most complex scenario. */
  @Test
  void testBaseUrlMolgenisSubdir8080() {
    runTestConfig(
        (ctx) -> contextWrapper(ctx, URLUtils.extractBaseURL(ctx).getBytes()),
        "http://molgenis.org:8080/subdir/",
        "molgenis.org:8080",
        8080,
        "/subdir");
  }

  @Test
  void testContextPathTrailingSlash() {
    runTestConfig(
        (ctx) -> contextWrapper(ctx, URLUtils.extractBaseURL(ctx).getBytes()),
        "http://molgenis.org:8080/subdir/",
        "molgenis.org:8080",
        8080,
        "/subdir/");
  }

  @Test
  void testMismatchingHostPort() {
    // Fails if using ctx.port() instead of splitting ctx.host()
    runTestConfig(
        (ctx) -> contextWrapper(ctx, URLUtils.extractBaseURL(ctx).getBytes()),
        "http://molgenis.org:8081/",
        "molgenis.org:8081",
        80,
        null);
  }

  @Test
  void testMismatchingHostPort8080() {
    // Does not fail if using ctx.port() instead of splitting ctx.host()
    runTestConfig(
        (ctx) -> contextWrapper(ctx, URLUtils.extractBaseURL(ctx).getBytes()),
        "http://molgenis.org:8081/",
        "molgenis.org:8081",
        8080,
        null);
  }
}
