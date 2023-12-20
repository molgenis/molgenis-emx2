package org.molgenis.emx2.rdf;

import java.net.URI;
import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.IRI;
import org.molgenis.emx2.MolgenisException;

public class RDFUtils {
  private RDFUtils() {
    // static only
  }

  /** Extract the host location from a request URI. */
  public static String extractHost(URI requestURI) {
    return requestURI.getScheme()
        + "://"
        + requestURI.getHost()
        + (requestURI.getPort() != -1 ? ":" + requestURI.getPort() : "");
  }

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
}
