package org.molgenis.emx2.utils;

import java.net.URI;
import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.molgenis.emx2.MolgenisException;

public class URIUtils {
  private URIUtils() {
    // static only
  }

  /** Extract the host location from a request URI. */
  public static String extractHost(URI requestURI) {
    return requestURI.getScheme()
        + "://"
        + requestURI.getHost()
        + (requestURI.getPort() != -1 ? ":" + requestURI.getPort() : "");
  }

  public static IRI encodeIRI(String iriString) {
    return Values.iri(ParsedIRI.create(iriString).normalize().toASCIIString());
  }

  /**
   * Old function used by FDP API (does NOT normalize nor use ASCIIString!)
   */
  public static IRI encodedIRI(String uriString) {
    return org.eclipse.rdf4j.model.util.Values.iri(ParsedIRI.create(uriString).toString());
  }

  public static URI getURI(String uriString) {
    try {
      ParsedIRI parsedIRI = ParsedIRI.create(uriString);
      return new URI(
          parsedIRI.getScheme(),
          parsedIRI.getUserInfo(),
          parsedIRI.getHost(),
          parsedIRI.getPort(),
          parsedIRI.getPath(),
          parsedIRI.getQuery(),
          parsedIRI.getFragment());
    } catch (Exception e) {
      throw new MolgenisException("getURI failed", e);
    }
  }

  public static String extractHost(String uriString) {
    URI uri = getURI(uriString);
    return extractHost(uri);
  }
}
