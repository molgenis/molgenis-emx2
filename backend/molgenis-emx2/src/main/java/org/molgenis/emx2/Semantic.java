package org.molgenis.emx2;

import static java.util.Objects.requireNonNull;

/**
 * Represents a semantic field and stores it in a list of 1 element. Full IRI's are surrounded by a
 * {@code <} and {@code >}, while a prefixed name is simply {@code prefix:localName}.
 *
 * <p>A {@link Semantic} object should be processed by a {@link SchemaMetadata}-specific {@link
 * SemanticPrefixes} to ensure the correct prefixes are applied before further usage.
 */
public class Semantic {
  private final String sequencePath;

  Semantic(String semantic) {
    if (!semantic.contains(":")) throw new MolgenisException("Invalid semantic: " + semantic);
    if (semantic.startsWith("<")) {
      if (!semantic.endsWith(">")) {
        throw new MolgenisException("Invalid semantic IRI: " + semantic);
      }
    } else {
      if (semantic.substring(semantic.indexOf(':') + 1).isEmpty()) {
        throw new MolgenisException("Prefixed name misses local part: " + semantic);
      }
    }
    this.sequencePath = requireNonNull(semantic);
  }

  String get() {
    return sequencePath;
  }

  /**
   * String representation of the semantic field. IMPORTANT: This representation is not validated on
   * correct prefixes!
   */
  @Override
  public String toString() {
    return String.join("/", sequencePath);
  }
}
