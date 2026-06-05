package org.molgenis.emx2;

import static java.util.Objects.requireNonNull;

import java.util.List;
import org.eclipse.rdf4j.model.IRI;

public class Semantic {
  private final SemanticPrefixes prefixes;
  private final String semantic;

  Semantic(final SemanticPrefixes prefixes, String semantic) {
    this.prefixes = requireNonNull(prefixes);
    this.semantic = requireNonNull(semantic);
  }

  /**
   * @see SemanticPrefixes#map(String)
   */
  public List<IRI> asIRI() {
    return prefixes.map(semantic);
  }

  /**
   * @see SemanticPrefixes#mapAsStrings(String)
   */
  public List<String> asString() {
    return prefixes.mapAsStrings(semantic);
  }

  /**
   * @see SemanticPrefixes#mapAsOptimizedStrings(String)
   */
  public List<String> asOptimizedString() {
    return prefixes.mapAsOptimizedStrings(semantic);
  }

  /**
   * String representation of the semantic field. IMPORTANT: This representation is not validated!
   */
  @Override
  public String toString() {
    return semantic;
  }
}
