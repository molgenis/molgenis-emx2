package org.molgenis.emx2.web.matomo;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.molgenis.emx2.Constants.MATOMO_URL_ENV;

import com.sun.net.httpserver.HttpServer;
import io.javalin.http.Context;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
class MatomoTrackingServiceTest {

  private static final String REQUEST_URL = "http://localhost:8080";

  private static final List<URI> requests = Collections.synchronizedList(new ArrayList<>());

  private static HttpServer matomoServer;

  @SystemStub
  private final EnvironmentVariables env =
      new EnvironmentVariables(
          MATOMO_URL_ENV, "http://localhost:" + matomoServer.getAddress().getPort());

  @BeforeAll
  static void startMatomoServer() throws IOException {
    matomoServer = HttpServer.create(new InetSocketAddress(0), 0);
    matomoServer.createContext(
        "/matomo.php",
        exchange -> {
          requests.add(exchange.getRequestURI());
          exchange.sendResponseHeaders(204, -1);
          exchange.close();
        });
    matomoServer.start();
  }

  @AfterAll
  static void stopMatomoServer() {
    matomoServer.stop(0);
  }

  @BeforeEach
  void clearRequests() {
    requests.clear();
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
    MatomoTrackingService.track(requestWithUserAgent(userAgent));
    await().atMost(Duration.ofSeconds(8)).until(() -> requests.size() == 1);
  }

  @Test
  void shouldSendContextAsBotRequest() {
    MatomoTrackingService.track(requestWithUserAgent("ChatGPT-User"));

    await().atMost(Duration.ofSeconds(8)).until(() -> requests.size() == 1);
    String query = requests.getFirst().getQuery();
    assertTrue(query.contains("url=" + REQUEST_URL));
    assertTrue(query.contains("recMode=2"));
  }

  @Test
  void shouldDiscardNonAiRequest() {
    MatomoTrackingService.track(
        requestWithUserAgent(
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:157.0) Gecko/20100101 Firefox/157.0"));
    await().atMost(Duration.ofSeconds(3)).until(requests::isEmpty);
  }

  private static Context requestWithUserAgent(String userAgent) {
    Context ctx = mock(Context.class);
    doReturn(userAgent).when(ctx).userAgent();
    doReturn(REQUEST_URL).when(ctx).fullUrl();
    return ctx;
  }
}
