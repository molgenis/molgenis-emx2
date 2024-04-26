package org.molgenis.emx2.fairdatapoint;

import java.util.Set;
import java.util.TreeSet;

public class FormatMimeTypes {

  /** Possible download formats for EMX2 tables */
  public static final Set<String> FORMATS =
      new TreeSet<>(Set.of("csv", "ttl", "excel", "zip", "graphql"));

  /**
   * Convert a format into its corresponding MIME type
   *
   * @param format
   * @return
   * @throws Exception
   */
  public static String formatToMediaType(String format) throws Exception {
    String mediaType;
    switch (format) {
      case "csv":
        mediaType = "https://www.iana.org/assignments/media-types/text/csv";
        break;
      case "graphql":
        mediaType = "https://www.iana.org/assignments/media-types/application/ld+json";
        break;
      case "ttl":
        mediaType = "https://www.iana.org/assignments/media-types/text/turtle";
        break;
      case "excel":
        mediaType = "https://www.iana.org/assignments/media-types/application/vnd.ms-excel";
        break;
      case "zip":
        mediaType = "https://www.iana.org/assignments/media-types/application/zip";
        break;
      default:
        throw new Exception("MIME Type could not be assigned");
    }
    return mediaType;
  }
}
