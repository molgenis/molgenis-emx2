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
 * Class that contains the needed code to process a {@link Semantic} object into a usable {@link
 * List} that is validated on the schema-specific {@link Namespace}{@code s}.
 *
 * <p>While any IRI (defined by '<' and '>') should already be validated by {@link Semantic} (and
 * some validation is done on prefixed names as well), only after processing through this class it
 * can be assured that the defined prefixed names are valid for the given schema (or list of
 * namespaces).
 */
public class SemanticPrefixes {
  private static final Logger logger = LoggerFactory.getLogger(SemanticPrefixes.class);

  private static final String SEMANTIC_PREFIXES_NAME_PREFIX = "prefix";
  private static final String SEMANTIC_PREFIXES_NAME_IRI = "iri";

  private static final Map<String, Namespace> DEFAULT_NAMESPACES_MAP =
      DefaultNamespace.streamAll().collect(Collectors.toMap(Namespace::getPrefix, i -> i));

  private static final CsvSchema SEMANTIC_PREFIXES_CSV_SCHEMA =
      CsvSchema.builder()
          .addColumn(SEMANTIC_PREFIXES_NAME_PREFIX)
          .addColumn(SEMANTIC_PREFIXES_NAME_IRI)
          .build();

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

  public Set<Namespace> getAllNamespaces() {
    return Set.copyOf(namespaces.values());
  }

  /**
   * @param semantic The semantic object as passed through a public method that needs to be parsed.
   * @param iriOperator Processes an IRI. Input can be assumed to be valid due to {@link Semantic}
   *     already validating this.
   * @param prefixedNameOperator Processes a prefixed name. While the format should be valid, ensure
   *     {@link #getNamespace(String)} is called so that undefined prefixes will throw an error!
   * @return a {@link List}{@code <R>} with the parsed values
   * @param <R> the return type of {@code iriOperator} and {@code prefixedNameOperator}
   */
  private <R> List<R> map(
      final Semantic semantic,
      final Function<String, R> iriOperator,
      final Function<String, R> prefixedNameOperator) {
    return semantic.getSequencePath().stream()
        .map(
            sequencePathItem ->
                sequencePathItem.startsWith("<")
                    ? iriOperator.apply(
                        sequencePathItem.substring(1, sequencePathItem.length() - 1))
                    : prefixedNameOperator.apply(sequencePathItem))
        .toList();
  }

  private Namespace getNamespace(final String prefix) {
    Namespace foundNamespace = namespaces.get(prefix);
    if (foundNamespace == null)
      throw new MolgenisException("Semantic uses an undefined prefix label: " + prefix);
    return foundNamespace;
  }

  /** Maps a semantic to a list of {@link IRI}{@code s} that represent a sequence path. */
  public List<IRI> mapAsIri(final Semantic semantic) {
    return map(
        semantic,
        Values::iri,
        prefixedName -> {
          String[] split = prefixedName.split(":");
          return Values.iri(getNamespace(split[0]), split[1]);
        });
  }

  /** Maps a semantic to a list of {@link String}{@code s} that represent a sequence path. */
  public List<String> mapAsString(final Semantic semantic) {
    return map(
        semantic,
        "<%s>"::formatted,
        prefixedName -> {
          getNamespace(prefixedName.split(":")[0]);
          return prefixedName;
        });
  }
}
