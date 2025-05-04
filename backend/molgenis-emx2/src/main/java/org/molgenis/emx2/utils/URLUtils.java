package org.molgenis.emx2.utils;

import io.javalin.http.Context;
import java.util.Objects;

public class URLUtils {
  public static String extractBaseURL(Context ctx) {
    String host = Objects.requireNonNull(ctx.host());
    String[] hostSplit = host.split(":", 2);
    if (hostSplit.length == 2 && isDefaultPort(ctx.scheme(), hostSplit[1])) {
      host = hostSplit[0];
    }

    return ctx.scheme()
        + "://"
        + host
        // ctx.contextPath() should start with "/"
        + (!ctx.contextPath().isEmpty() ? ctx.contextPath() : "");
  }

  public static boolean isDefaultPort(String scheme, String port) {
    return (scheme.equals("http") && port.equals("80"))
        || (scheme.equals("https") && port.equals("443"));
  }
}
