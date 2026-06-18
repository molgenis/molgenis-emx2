package org.molgenis.emx2;

import static org.molgenis.emx2.Constants.SETTING_SEMANTIC_PREFIXES;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.util.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that contains the needed code to process the semantic field into a usable object. It keeps
 * account for both any schema-specific custom prefixes if a sequence path is used. <br>
 * <br>
 * Examples of allowed values for the semantic field:
 *
 * <ul>
 *   <li>
 *       <pre><code><http://example.com/first>/<http://example.com/second></code></pre>
 *   <li>
 *       <pre><code>myPrefix:first/myPrefix:second</code></pre>
 *   <li>
 *       <pre><code><http://example.com/first>/myPrefix:second</code></pre>
 * </ul>
 */
public class SemanticPrefixes {
  private static final Logger logger = LoggerFactory.getLogger(SemanticPrefixes.class);

  private static final String SEMANTIC_PREFIXES_NAME_PREFIX = "prefix";
  private static final String SEMANTIC_PREFIXES_NAME_IRI = "iri";

  private static final Map<String, Namespace> DEFAULT_NAMESPACES_MAP =
      DefaultNamespace.streamAll().collect(Collectors.toMap(Namespace::getPrefix, i -> i));

  private static final List<String> LEGACY_FORBIDDEN_PREFIXES =
      List.of("http", "https", "urn", "tag");

  private static final CsvSchema SEMANTIC_PREFIXES_CSV_SCHEMA =
      CsvSchema.builder()
          .addColumn(SEMANTIC_PREFIXES_NAME_PREFIX)
          .addColumn(SEMANTIC_PREFIXES_NAME_IRI)
          .build();

  // TreeMap for consistency in case it's used for generating output
  private final Map<String, Namespace> namespaces;

  public SemanticPrefixes(Namespace... namespaces) {
    this.namespaces =
        Arrays.stream(namespaces)
            .map(namespace -> Map.entry(namespace.getPrefix(), namespace))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public SemanticPrefixes(Schema schema) {
    this(schema.getMetadata());
  }

  public SemanticPrefixes(SchemaMetadata schemaMetadata) {
    Map<String, Namespace> namespaces;
    if (!schemaMetadata.getSettings().containsKey(SETTING_SEMANTIC_PREFIXES)) {
      namespaces = DEFAULT_NAMESPACES_MAP;
    } else {
      try {
        Set<Namespace> namespaceSet = getCustomPrefixes(schemaMetadata);
        namespaces = namespaceSet.stream().collect(Collectors.toMap(Namespace::getPrefix, i -> i));
      } catch (IOException e) {
        logger.error("Failed to retrieve custom prefixes, falling back to default", e);
        namespaces = DEFAULT_NAMESPACES_MAP;
      }
    }
    this.namespaces = namespaces;
  }

  private static Set<Namespace> getCustomPrefixes(SchemaMetadata schema) throws IOException {
    Set<Namespace> namespaces = new HashSet<>();
    try (MappingIterator<Map<String, String>> iterator =
        new CsvMapper()
            .readerForMapOf(String.class)
            .with(SEMANTIC_PREFIXES_CSV_SCHEMA)
            .readValues(schema.getSetting(SETTING_SEMANTIC_PREFIXES))) {

      iterator.forEachRemaining(
          i -> {
            Namespace namespace =
                Values.namespace(
                    i.get(SEMANTIC_PREFIXES_NAME_PREFIX), i.get(SEMANTIC_PREFIXES_NAME_IRI));
            try {
              Values.iri(namespace, "test");
            } catch (Exception e) {
              throw new MolgenisException(
                  "Unusable IRI found in custom_prefixes of %s: %s,%s"
                      .formatted(schema.getName(), namespace.getPrefix(), namespace.getName()));
            }
            namespaces.add(namespace);
          });
    }
    return namespaces;
  }

  public Set<Namespace> getAllNamespaces() {
    return Set.copyOf(namespaces.values());
  }

  /**
   * Support for old semantic format where an IRI did not need to be surrounded by {@code <} and
   * {@code >}. Note that this old format does not allow sequence paths and assumes the semantic
   * field contains only 1 semantic part. We should deprecate this ASAP to prevent unexpected
   * behavior!
   *
   * @return the IRI as {@code <R>} or null if there was no legacy format found.
   */
  private <R> R legacyMap(final String semantic, Function<String, R> iriOperator) {
    for (String prefix : LEGACY_FORBIDDEN_PREFIXES) {
      if (semantic.startsWith(prefix) && !namespaces.containsKey(prefix)) {
        return iriOperator.apply(semantic);
      }
    }
    return null;
  }

  /**
   * To ensure the semantic is validated, make sure {@link #processIri(String)} & {@link
   * #processPrefixedName(String)} are called witin the {@code iriOperator} & {@code
   * prefixedNameOperator} respectively.
   */
  private <R> List<R> map(
      final String semantic,
      Function<String, R> iriOperator,
      Function<String, R> prefixedNameOperator) {
    // Legacy support
    R sequenceItem = legacyMap(semantic, iriOperator);
    if (sequenceItem != null) return Collections.singletonList(sequenceItem);

    // New semantic field format.
    List<R> sequencePath = new ArrayList<>();

    int sequenceItemStart = 0;
    boolean foundIri = false;
    int length = semantic.length();
    for (int i = 0; i < length; i++) {
      char curChar = semantic.charAt(i);

      switch (curChar) {
        case '<' -> {
          if (foundIri)
            throw new MolgenisException(
                "Invalid semantic: Found new IRI opening bracket ('<') before previous IRI was closed.");
          sequenceItemStart = i + 1;
          foundIri = true;
        }
        case '>' -> {
          if (!foundIri)
            throw new MolgenisException(
                "Invalid semantic: IRI closing bracket ('>') without opening bracket ('<').");
          if (i + 1 != length && semantic.charAt(i + 1) != '/')
            throw new MolgenisException(
                "Invalid semantic: Missing sequence path separator ('/') after IRI closing bracket ('>').");
          sequencePath.add(iriOperator.apply(semantic.substring(sequenceItemStart, i)));
          sequenceItemStart = i + 2;
          i++;
          foundIri = false;
        }
        case '/' -> {
          if (!foundIri) {
            sequencePath.add(prefixedNameOperator.apply(semantic.substring(sequenceItemStart, i)));
            sequenceItemStart = i + 1;
          }
        }
      }
    }
    // Ensure last item is added when not IRI
    if (semantic.charAt(length - 1) != '>') {
      sequencePath.add(prefixedNameOperator.apply(semantic.substring(sequenceItemStart, length)));
    }

    return sequencePath;
  }

  private IRI processIri(final String semanticPart) {
    try {
      return Values.iri(semanticPart);
    } catch (IllegalArgumentException e) {
      throw new MolgenisException("Found IRI is malformed:" + semanticPart, e);
    }
  }

  private IRI processPrefixedName(final String semanticPart) {
    String[] prefixedNameSplit = semanticPart.split(":");
    if (prefixedNameSplit.length != 2)
      throw new MolgenisException("Could not split prefixed name into prefix label & local part");
    Namespace namespace = namespaces.get(prefixedNameSplit[0]);
    if (namespace == null)
      throw new MolgenisException(
          "Semantic uses a non-defined prefix label: " + prefixedNameSplit[0]);
    try {
      return Values.iri(namespace, prefixedNameSplit[1]);
    } catch (IllegalArgumentException e) {
      throw new MolgenisException(
          "Could not generate valid IRI from prefixed name: " + semanticPart, e);
    }
  }

  /** Maps a semantic to a list of {@link IRI}{@code s} that represent a sequence path. */
  public List<IRI> map(final String semantic) {
    return map(semantic, this::processIri, this::processPrefixedName);
  }

  /** Maps a semantic to a list of {@link String}{@code s} that represent a sequence path. */
  public List<String> mapAsStrings(final String semantic) {
    return map(
        semantic,
        iri -> {
          processIri(iri);
          return "<%s>".formatted(iri);
        },
        prefixedName -> {
          processPrefixedName(prefixedName);
          return prefixedName;
        });
  }
}
