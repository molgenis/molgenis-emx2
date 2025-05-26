package org.molgenis.emx2.web.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class EncodingHelpers {

  private EncodingHelpers() {}

  // Encodes a path segment (e.g., one part between slashes in a URL)
  public static String encodePathSegment(String segment) {
    StringBuilder encoded = new StringBuilder();
    for (char c : segment.toCharArray()) {
      if (isUnreserved(c)) {
        encoded.append(c);
      } else {
        encoded.append('%');
        encoded.append(String.format("%02X", (int) c));
      }
    }
    return encoded.toString();
  }

  // Encodes query parameter names or values (space becomes +, etc.)
  public static String encodeQueryParam(String param) {
    return URLEncoder.encode(param, StandardCharsets.UTF_8);
  }

  private static boolean isUnreserved(char c) {
    // RFC 3986: ALPHA / DIGIT / "-" / "." / "_" / "~"
    return (c >= 'a' && c <= 'z')
        || (c >= 'A' && c <= 'Z')
        || (c >= '0' && c <= '9')
        || c == '-'
        || c == '.'
        || c == '_'
        || c == '~';
  }
}
