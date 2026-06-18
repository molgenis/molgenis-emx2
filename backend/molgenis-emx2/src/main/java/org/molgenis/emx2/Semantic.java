package org.molgenis.emx2;

import static java.util.Objects.requireNonNull;

import java.util.List;
import org.eclipse.rdf4j.model.IRI;

public class Semantic {
  private static final IllegalStateException NO_PREFIXES_EXCEPTION =
      new IllegalStateException("Only toString() is allowed when no prefixes are defined.");

  private final SemanticPrefixes prefixes;
  private final String semantic;

  Semantic(String semantic) {
    this.prefixes = null;
    this.semantic = requireNonNull(semantic);
  }

  Semantic(final SemanticPrefixes prefixes, String semantic) {
    this.prefixes = requireNonNull(prefixes);
    this.semantic = requireNonNull(semantic);
  }

  SemanticPrefixes getPrefixes() {
    return prefixes;
  }

  /**
   * @see SemanticPrefixes#map(String)
   */
  public List<IRI> asIRI() {
    if (prefixes == null) throw NO_PREFIXES_EXCEPTION;
    return prefixes.map(semantic);
  }

  /**
   * @see SemanticPrefixes#mapAsStrings(String)
   */
  public List<String> asString() {
    if (prefixes == null) throw NO_PREFIXES_EXCEPTION;
    return prefixes.mapAsStrings(semantic);
  }

  /**
   * String representation of the semantic field. IMPORTANT: This representation is not validated!
   */
  @Override
  public String toString() {
    return semantic;
  }
}
