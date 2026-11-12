package org.molgenis.emx2.web;

import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.HandlerType;
import io.javalin.http.Header;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Locale;
import java.util.Set;

/**
 * Rejects state-changing requests that a browser sent on behalf of another site (CSRF). A browser
 * attaches the session cookie to cross-site form posts, so for these methods we require that the
 * Origin (or, when absent, the Referer) points to this server. Requests without either header
 * (scripts, curl) are allowed. Token headers do not exempt a request: when a session exists it
 * takes precedence over the token, so a token header alone says nothing about how the request is
 * authenticated.
 */
final class CsrfFilter {

  private static final Set<HandlerType> STATE_CHANGING_METHODS =
      Set.of(HandlerType.POST, HandlerType.PUT, HandlerType.PATCH, HandlerType.DELETE);
  static final String X_FORWARDED_HOST = "X-Forwarded-Host";

  private CsrfFilter() {
    // hide constructor
  }

  static void check(Context ctx) {
    if (!STATE_CHANGING_METHODS.contains(ctx.method())) {
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

  /** Compares host names only; ports may differ behind a reverse proxy. */
  private static boolean isOwnHost(String sourceHost, Context ctx) {
    String forwardedHost = ctx.header(X_FORWARDED_HOST);
    if (forwardedHost != null && sourceHost.equals(hostOfAuthority(forwardedHost.split(",")[0]))) {
      return true;
    }
    String host = ctx.header(Header.HOST);
    return (host != null && sourceHost.equals(hostOfAuthority(host)))
        || (MolgenisWebservice.hostUrl != null
            && sourceHost.equalsIgnoreCase(MolgenisWebservice.hostUrl.getHost()));
  }

  private static String hostOf(String url) {
    // only the host is needed, and URI rejects characters browsers do send in a path or query
    String schemeAndAuthority = url.trim().replaceFirst("(://[^/?#]*).*", "$1");
    try {
      String host = URI.create(schemeAndAuthority).toURL().getHost();
      return host.isEmpty() ? null : host.toLowerCase(Locale.ROOT);
    } catch (IllegalArgumentException | MalformedURLException e) {
      return null;
    }
  }

  private static String hostOfAuthority(String authority) {
    return hostOf("http://" + authority.trim());
  }
}
