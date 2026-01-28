package org.molgenis.emx2.fairmapper;

import java.util.List;
import java.util.Map;

public class ContentNegotiator {
  private static final List<String> ACCEPT_PRIORITY =
      List.of("text/turtle", "application/ld+json", "application/n-triples", "application/json");

  private static final Map<String, String> MIME_TO_FORMAT =
      Map.of(
          "text/turtle", "turtle",
          "application/ld+json", "jsonld",
          "application/n-triples", "ntriples",
          "application/json", "json");

  private static final Map<String, String> FORMAT_TO_MIME =
      Map.of(
          "turtle", "text/turtle",
          "jsonld", "application/ld+json",
          "ntriples", "application/n-triples",
          "json", "application/json");

  public static String resolveOutputFormat(String acceptHeader, String defaultFormat) {
    if (acceptHeader == null || acceptHeader.isBlank()) return defaultFormat;
    String accept = acceptHeader.toLowerCase();
    for (String mime : ACCEPT_PRIORITY) {
      if (accept.contains(mime)) return MIME_TO_FORMAT.get(mime);
    }
    return defaultFormat;
  }

  public static String getMimeType(String format) {
    return FORMAT_TO_MIME.getOrDefault(format.toLowerCase(), "application/json");
  }

  public static boolean isRdfFormat(String format) {
    return format != null && List.of("turtle", "jsonld", "ntriples").contains(format.toLowerCase());
  }
}
