package org.molgenis.emx2.utils;

import static org.molgenis.emx2.Constants.HYPERLINK_REGEX;

import io.javalin.http.Context;
import java.util.Objects;
import java.util.regex.Pattern;

public class URLUtils {
  private static final Pattern pattern = Pattern.compile(HYPERLINK_REGEX);

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

  public static String validateUrl(String urlString) {
    if (!pattern.matcher(urlString).matches())
      throw new IllegalArgumentException(urlString + " is not valid");
    return urlString;
  }
}
