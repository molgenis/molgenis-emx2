package org.molgenis.emx2;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.rdf4j.model.util.Values;

/**
 * Represents a semantic field and splits up a sequence path into its individual components.
 * Examples of allowed semantic fields are:
 *
 * <ul>
 *   <li>
 *       <pre><code><http://example.com/first>/<http://example.com/second></code></pre>
 *   <li>
 *       <pre><code>myPrefix:first/myPrefix:second</code></pre>
 *   <li>
 *       <pre><code><http://example.com/first>/myPrefix:second</code></pre>
 * </ul>
 *
 * A {@link Semantic} object should be processed by a {@link SchemaMetadata}-specific {@link
 * SemanticPrefixes} to ensure the correct prefixes are applied before further usage.
 */
public class Semantic {
  private final List<String> sequencePath;

  Semantic(String semantic) {
    this.sequencePath = split(requireNonNull(semantic));
  }

  List<String> getSequencePath() {
    return sequencePath;
  }

  private List<String> split(final String semantic) {
    List<String> sequencePath = new ArrayList<>();

    int sequenceItemStart = 0;
    boolean foundIri = false;
    int length = semantic.length();
    for (int i = 0; i < length; i++) {
      char curChar = semantic.charAt(i);

      switch (curChar) {
        case '<' -> {
          if (foundIri)
            throw new MolgenisException(
                "Found new IRI opening bracket ('<') before previous IRI was closed: " + semantic);
          foundIri = true;
        }
        case '>' -> {
          if (!foundIri)
            throw new MolgenisException(
                "IRI closing bracket ('>') without opening bracket ('<'): " + semantic);
          if (i + 1 != length && semantic.charAt(i + 1) != '/')
            throw new MolgenisException(
                "Missing sequence path separator ('/') after IRI closing bracket ('>'): "
                    + semantic);
          sequencePath.add(validateIri(semantic.substring(sequenceItemStart, i + 1)));
          sequenceItemStart = i + 2;
          i++;
          foundIri = false;
        }
        case '/' -> {
          if (!foundIri) {
            if (semantic.charAt(i - 1) == ':') {
              throw new MolgenisException(
                  "Found '/' after ':' outside of brackets ('<' & '>'): " + semantic);
            }
            sequencePath.add(validatePrefixedName(semantic.substring(sequenceItemStart, i)));
            sequenceItemStart = i + 1;
          }
        }
      }
    }
    // Ensure last item is processed
    if (semantic.charAt(length - 1) != '>') {
      if (foundIri) {
        throw new MolgenisException(
            "Invalid semantic: Missing closing bracket ('>') for opening bracket ('<').");
      } else {
        sequencePath.add(validatePrefixedName(semantic.substring(sequenceItemStart, length)));
      }
    }

    return sequencePath;
  }

  private String validateIri(final String semanticPart) {
    try {
      Values.iri(semanticPart.substring(1, semanticPart.length() - 1));
    } catch (IllegalArgumentException e) {
      throw new MolgenisException("Found malformed IRI:" + semanticPart, e);
    }
    return semanticPart;
  }

  /** Only validates formatting, not whether the used prefix is valid. */
  private String validatePrefixedName(final String semanticPart) {
    String[] prefixedNameSplit = semanticPart.split(":");
    if (prefixedNameSplit.length != 2)
      throw new MolgenisException(
          "Could not split prefixed name into prefix label & local part: " + semanticPart);
    if (prefixedNameSplit[1].isEmpty())
      throw new MolgenisException("Local part of prefixed name must not be empty: " + semanticPart);
    return semanticPart;
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
