package org.molgenis.emx2;

import java.util.List;
import org.eclipse.rdf4j.model.IRI;

public record Semantic(SemanticPrefixes prefixes, String rawString) {

  public List<IRI> asIRI() {
    return prefixes.map(rawString);
  }

  public List<String> asString() {
    return prefixes.mapAsString(rawString);
  }

  public List<String> asPrefixedNames() {
    return prefixes.mapAsPrefixedName(rawString);
  }
}
