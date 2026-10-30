package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.Constants.MOLGENIS_TOKEN;

import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.HandlerType;
import io.javalin.http.Header;
import java.net.URI;
import java.util.Set;

/**
 * Rejects state-changing requests that a browser sent on behalf of another site (CSRF). A browser
 * attaches the session cookie to cross-site form posts, so for these methods we require that the
 * Origin (or, when absent, the Referer) points to this server. Requests without either header
 * (scripts, curl) and requests authenticated with a token header (which a cross-site form cannot
 * set) are allowed.
 */
class CsrfFilter {

  private static final Set<HandlerType> STATE_CHANGING_METHODS =
      Set.of(HandlerType.POST, HandlerType.PUT, HandlerType.PATCH, HandlerType.DELETE);
  static final String X_FORWARDED_HOST = "X-Forwarded-Host";

  private CsrfFilter() {
    // hide constructor
  }

  static void check(Context ctx) {
    if (!STATE_CHANGING_METHODS.contains(ctx.method()) || hasTokenHeader(ctx)) {
      return;
    }
    String source = ctx.header(Header.ORIGIN);
    if (source == null) {
      source = ctx.header(Header.REFERER);
    }
    if (source == null) {
      return;
    }
    String sourceHost = hostOf(source);
    if (sourceHost == null || !isOwnHost(sourceHost, ctx)) {
      throw new ForbiddenResponse("Cross-site request rejected: origin does not match this server");
    }
  }

  private static boolean hasTokenHeader(Context ctx) {
    for (String tokenHeader : MOLGENIS_TOKEN) {
      if (ctx.header(tokenHeader) != null) {
        return true;
      }
    }
    return false;
  }

  /** Compares host names only; ports may differ behind a reverse proxy. */
  private static boolean isOwnHost(String sourceHost, Context ctx) {
    String forwardedHost = ctx.header(X_FORWARDED_HOST);
    if (forwardedHost != null && sourceHost.equals(hostOfAuthority(forwardedHost.split(",")[0]))) {
      return true;
    }
    String host = ctx.header(Header.HOST);
    if (host != null && sourceHost.equals(hostOfAuthority(host))) {
      return true;
    }
    return MolgenisWebservice.hostUrl != null
        && sourceHost.equalsIgnoreCase(MolgenisWebservice.hostUrl.getHost());
  }

  private static String hostOf(String url) {
    try {
      String host = URI.create(url.trim()).getHost();
      return host == null ? null : host.toLowerCase();
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static String hostOfAuthority(String authority) {
    return hostOf("http://" + authority.trim());
  }
}
