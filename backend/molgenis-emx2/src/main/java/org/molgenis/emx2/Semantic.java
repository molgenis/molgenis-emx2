package org.molgenis.emx2;

import static java.util.Objects.requireNonNull;

/**
 * Represents a semantic field and stores it in a list of 1 element. This can either be:
 * <ul>
 *   <li>An IRI that starts with http(s)</li>
 *   <li>An IRI that is surrounded by {@code <} and {@code >} (can use any IRI scheme)</li>
 *   <li>A prefixed name (if not starting with http(s) nor with {@code <})</li>
 * </ul>
 *
 * <p>A {@link Semantic} object should be processed by a {@link SchemaMetadata}-specific {@link
 * SemanticPrefixes} before further processing (unless it's purely for storing/retrieving the data).
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
      if (isLegacyIri(semantic)) {
        ColumnType.HYPERLINK.validate(semantic);
      } else if (semantic.substring(semantic.indexOf(':') + 1).isEmpty()) {
        throw new MolgenisException("Prefixed name misses local part: " + semantic);
      }
    }
    this.sequencePath = requireNonNull(semantic);
  }

  public boolean isLegacyIri() {
    return isLegacyIri(sequencePath);
  }

  private boolean isLegacyIri(String semantic) {
    return semantic.startsWith("http:") || semantic.startsWith("https:");
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
