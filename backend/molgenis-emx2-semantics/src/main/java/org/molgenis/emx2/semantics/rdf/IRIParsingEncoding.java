package org.molgenis.emx2.semantics.rdf;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.IRI;
import org.molgenis.emx2.MolgenisException;

public class IRIParsingEncoding {
  /**
   * @param uriString
   * @return
   * @throws URISyntaxException
   */
  public static URI getURI(String uriString) {
    try {
      ParsedIRI parsedIRI = ParsedIRI.create(uriString);
      URI uri =
          new URI(
              parsedIRI.getScheme(),
              parsedIRI.getUserInfo(),
              parsedIRI.getHost(),
              parsedIRI.getPort(),
              parsedIRI.getPath(),
              parsedIRI.getQuery(),
              parsedIRI.getFragment());
      return uri;
    } catch (Exception e) {
      throw new MolgenisException("getURI failed", e);
    }
  }

  /**
   * @param uriString
   * @return
   */
  public static IRI encodedIRI(String uriString) {
    return org.eclipse.rdf4j.model.util.Values.iri(ParsedIRI.create(uriString).toString());
  }
}
