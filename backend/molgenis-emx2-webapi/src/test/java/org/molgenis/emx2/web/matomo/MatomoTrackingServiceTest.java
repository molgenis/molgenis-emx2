package org.molgenis.emx2.web.matomo;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.sun.net.httpserver.HttpServer;
import io.javalin.http.Context;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MatomoTrackingServiceTest {

  private static final String SERVICE_URL = "http://localhost:8080";

  private static final List<URI> requests = Collections.synchronizedList(new ArrayList<>());
  private static final AtomicBoolean requested = new AtomicBoolean(false);

  private static HttpServer matomoServer;

  private static MatomoTrackingService service;

  @BeforeAll
  static void startMatomoServer() throws IOException {
    matomoServer = HttpServer.create(new InetSocketAddress(0), 0);
    matomoServer.createContext(
        "/matomo.php",
        exchange -> {
          requests.add(exchange.getRequestURI());
          requested.set(true);
          exchange.sendResponseHeaders(204, -1);
          exchange.close();
        });
    matomoServer.start();
    service = new MatomoTrackingService(matomoUrl(), 1, SERVICE_URL);
  }

  @AfterAll
  static void stopMatomoServer() {
    matomoServer.stop(0);
  }

  @BeforeEach
  void clearRequests() {
    requests.clear();
    requested.set(false);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "ChatGPT-User",
        "MistralAI-User",
        "Gemini-Deep-Research",
        "Claude-User",
        "Perplexity-User",
        "Google-NotebookLM",
        "prefix-ChatGPT-User-suffix"
      })
  void shouldSendRequestOnAiUserAgents(String userAgent) {
    service.track(requestWithUserAgent(userAgent));
    await().atMost(Duration.ofSeconds(8)).until(() -> requests.size() == 1);
  }

  @Test
  void shouldSendContextAsBotRequest() {
    service.track(requestWithUserAgent("ChatGPT-User"));

    await().atMost(Duration.ofSeconds(8)).until(() -> requests.size() == 1);
    String query = requests.getFirst().getQuery();
    assertTrue(query.contains("url=" + SERVICE_URL + "/some-action"));
    assertTrue(query.contains("recMode=1"));
  }

  @Test
  void shouldDiscardNonAiRequest() {
    service.track(
        requestWithUserAgent(
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:157.0) Gecko/20100101 Firefox/157.0"));
    await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(3)).until(requests::isEmpty);
  }

  @Test
  void shouldUseServiceUrlInsteadOfRequestHost() {
    MatomoTrackingService proxied =
        new MatomoTrackingService(matomoUrl(), 1, "https://example.org/");
    Context ctx = requestWithUserAgent("ChatGPT-User");
    doReturn("a=1&b=2").when(ctx).queryString();

    assertEquals("https://example.org/some-action?a=1&b=2", proxied.getRequestUrl(ctx));
  }

  @Test
  void shouldLeaveOutEmptyQueryString() {
    MatomoTrackingService proxied =
        new MatomoTrackingService(matomoUrl(), 1, "https://example.org");
    Context ctx = requestWithUserAgent("ChatGPT-User");

    assertEquals("https://example.org/some-action", proxied.getRequestUrl(ctx));
  }

  private static String matomoUrl() {
    return "http://localhost:" + matomoServer.getAddress().getPort();
  }

  private static Context requestWithUserAgent(String userAgent) {
    Context ctx = mock(Context.class);
    doReturn(userAgent).when(ctx).userAgent();
    doReturn("/some-action").when(ctx).path();
    return ctx;
  }
}
