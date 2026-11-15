package org.molgenis.emx2.web;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.HandlerType;
import io.javalin.http.Header;
import org.junit.jupiter.api.Test;

class CsrfFilterTest {

  private static Context request(HandlerType method, String host, String origin) {
    Context ctx = mock(Context.class);
    when(ctx.method()).thenReturn(method);
    when(ctx.header(Header.HOST)).thenReturn(host);
    when(ctx.header(Header.ORIGIN)).thenReturn(origin);
    return ctx;
  }

  @Test
  void givenCrossSiteOriginWhenPostThenRejected() {
    Context ctx = request(HandlerType.POST, "emx2.example.org", "https://evil.example");
    assertThrows(ForbiddenResponse.class, () -> CsrfFilter.check(ctx));
  }

  @Test
  void givenNullOriginWhenPostThenRejected() {
    Context ctx = request(HandlerType.POST, "emx2.example.org", "null");
    assertThrows(ForbiddenResponse.class, () -> CsrfFilter.check(ctx));
  }

  @Test
  void givenCrossSiteOriginWhenDeleteThenRejected() {
    Context ctx = request(HandlerType.DELETE, "emx2.example.org", "https://evil.example");
    assertThrows(ForbiddenResponse.class, () -> CsrfFilter.check(ctx));
  }

  @Test
  void givenSameOriginWhenPostThenAllowed() {
    Context ctx = request(HandlerType.POST, "emx2.example.org:8080", "https://emx2.example.org");
    assertDoesNotThrow(() -> CsrfFilter.check(ctx));
  }

  @Test
  void givenForwardedHostOriginWhenPostThenAllowed() {
    Context ctx = request(HandlerType.POST, "localhost:8080", "https://emx2.example.org");
    when(ctx.header(CsrfFilter.X_FORWARDED_HOST)).thenReturn("emx2.example.org, proxy.internal");
    assertDoesNotThrow(() -> CsrfFilter.check(ctx));
  }

  @Test
  void givenNoOriginOrRefererWhenPostThenAllowed() {
    Context ctx = request(HandlerType.POST, "emx2.example.org", null);
    assertDoesNotThrow(() -> CsrfFilter.check(ctx));
  }

  @Test
  void givenCrossSiteRefererWhenPostWithoutOriginThenRejected() {
    Context ctx = request(HandlerType.POST, "emx2.example.org", null);
    when(ctx.header(Header.REFERER)).thenReturn("https://evil.example/page");
    assertThrows(ForbiddenResponse.class, () -> CsrfFilter.check(ctx));
  }

  @Test
  void givenTokenHeaderWhenCrossSitePostThenRejected() {
    Context ctx = request(HandlerType.POST, "emx2.example.org", "https://evil.example");
    when(ctx.header(Constants.X_MOLGENIS_TOKEN)).thenReturn("some-token");
    assertThrows(ForbiddenResponse.class, () -> CsrfFilter.check(ctx));
  }

  @Test
  void givenTokenHeaderWithoutOriginWhenPostThenAllowed() {
    Context ctx = request(HandlerType.POST, "emx2.example.org", null);
    when(ctx.header(Constants.X_MOLGENIS_TOKEN)).thenReturn("some-token");
    assertDoesNotThrow(() -> CsrfFilter.check(ctx));
  }

  @Test
  void givenUnderscoreHostNameWhenPostThenAllowed() {
    Context ctx = request(HandlerType.POST, "molgenis_emx2:8080", "http://molgenis_emx2:8080");
    assertDoesNotThrow(() -> CsrfFilter.check(ctx));
  }

  @Test
  void givenRefererWithPipeInQueryWhenPostWithoutOriginThenAllowed() {
    Context ctx = request(HandlerType.POST, "emx2.example.org", null);
    when(ctx.header(Header.REFERER)).thenReturn("https://emx2.example.org/catalogue?filter=a|b");
    assertDoesNotThrow(() -> CsrfFilter.check(ctx));
  }

  @Test
  void givenIpv6HostWhenPostThenAllowed() {
    Context ctx = request(HandlerType.POST, "[::1]:8080", "http://[::1]:8080");
    assertDoesNotThrow(() -> CsrfFilter.check(ctx));
  }

  @Test
  void givenOwnHostAsSubdomainOfOtherSiteWhenPostThenRejected() {
    Context ctx =
        request(HandlerType.POST, "emx2.example.org", "https://emx2.example.org.evil.example");
    assertThrows(ForbiddenResponse.class, () -> CsrfFilter.check(ctx));
  }

  @Test
  void givenBackslashBeforeOwnHostWhenPostThenRejected() {
    Context ctx =
        request(HandlerType.POST, "emx2.example.org", "https://evil.example\\@emx2.example.org");
    assertThrows(ForbiddenResponse.class, () -> CsrfFilter.check(ctx));
  }

  @Test
  void givenCrossSiteOriginWhenGetThenAllowed() {
    Context ctx = request(HandlerType.GET, "emx2.example.org", "https://evil.example");
    assertDoesNotThrow(() -> CsrfFilter.check(ctx));
  }
}
