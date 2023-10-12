package org.molgenis.emx2.fairdatapoint;

import java.util.Set;
import java.util.TreeSet;
import org.molgenis.emx2.MolgenisException;

public class FormatMimeTypes {

  /** Possible download formats for EMX2 tables */
  public static final Set<String> FORMATS =
      new TreeSet<>(
          Set.of(
              "csv",
              "jsonld",
              "ttl",
              "excel",
              "zip",
              "rdf-ttl",
              "rdf-n3",
              "rdf-ntriples",
              "rdf-nquads",
              "rdf-xml",
              "rdf-trig",
              "rdf-jsonld",
              "graphql"));

  /**
   * Convert a format into its corresponding MIME type
   *
   * @param format
   * @return
   * @throws Exception
   */
  public static String formatToMediaType(String format) {
    String mediaType;
    switch (format) {
      case "csv":
        mediaType = "https://www.iana.org/assignments/media-types/text/csv";
        break;
      case "jsonld":
      case "rdf-jsonld":
      case "graphql":
        mediaType = "https://www.iana.org/assignments/media-types/application/ld+json";
        break;
      case "ttl":
      case "rdf-ttl":
        mediaType = "https://www.iana.org/assignments/media-types/text/turtle";
        break;
      case "excel":
        mediaType = "https://www.iana.org/assignments/media-types/application/vnd.ms-excel";
        break;
      case "zip":
        mediaType = "https://www.iana.org/assignments/media-types/application/zip";
        break;
      case "rdf-n3":
        mediaType = "https://www.iana.org/assignments/media-types/text/n3";
        break;
      case "rdf-ntriples":
        mediaType = "https://www.iana.org/assignments/media-types/application/n-triples";
        break;
      case "rdf-nquads":
        mediaType = "https://www.iana.org/assignments/media-types/application/n-quads";
        break;
      case "rdf-xml":
        mediaType = "https://www.iana.org/assignments/media-types/application/rdf+xml";
        break;
      case "rdf-trig":
        mediaType = "https://www.iana.org/assignments/media-types/application/trig";
        break;
      default:
        throw new MolgenisException("MIME Type could not be assigned");
    }
    return mediaType;
  }
}
