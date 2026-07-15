package org.molgenis.emx2;

import static java.util.Objects.requireNonNull;

import java.util.regex.Pattern;

/**
 * Represents a semantic field and stores it in a list of 1 element. Full IRI's are surrounded by a
 * {@code <} and {@code >}, while a prefixed name is simply {@code prefix:localName}.
 *
 * <p>A {@link Semantic} object should be processed by a {@link SchemaMetadata}-specific {@link
 * SemanticPrefixes} to ensure the correct prefixes are applied before further usage.
 */
public class Semantic {
  // Matches with Strings like "urn:" & "urn:uuid:", but allows variations such as "urnamespace:"
  private static final Pattern ILLEGAL_PREFIX = Pattern.compile("^(http(s)?|urn(:.*)?|tag):");

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
      if (isIllegalPrefix(semantic)) throw new MolgenisException("Invalid semantic: " + semantic);
    }
    this.sequencePath = requireNonNull(semantic);
  }

  String get() {
    return sequencePath;
  }

  /**
   * @param semantic a prefixed name as defined <a
   *     href="https://www.w3.org/TR/turtle/#prefixed-name">here</a>
   */
  private static boolean hasIllegalPrefix(String semantic) {
    return ILLEGAL_PREFIX.matcher(semantic).find();
  }

  /**
   * @param prefix the prefix WITHOUT ':' or anything after that. While needed in {@link
   *     SemanticPrefixes}, it will be deprecated right away so not worth creating Utils class (only
   *     exists due to splitting up sequence path into separate PR).
   */
  static boolean isIllegalPrefix(String prefix) {
    return hasIllegalPrefix(prefix + ':');
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
