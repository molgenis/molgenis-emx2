package org.molgenis.emx2.utils;

import java.net.URISyntaxException;
import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;

public class IRIUtils {
  public static IRI encodeIRI(String iriString) throws URISyntaxException {
    // ParsedIRI.create() suggests using constructor if creation is based on user-input (in our case
    // that would be table data).
    return Values.iri(new ParsedIRI(iriString).normalize().toASCIIString());
  }

  public static ParsedIRI decodeIRI(String iriString) throws URISyntaxException {
    return new ParsedIRI(iriString).normalize();
  }
}
