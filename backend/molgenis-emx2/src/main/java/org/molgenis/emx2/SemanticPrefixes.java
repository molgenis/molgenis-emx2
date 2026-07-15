package org.molgenis.emx2;

import static org.molgenis.emx2.Constants.SETTING_SEMANTIC_PREFIXES;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
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

  /**
   * @param schema The schema to retrieve custom semantic prefixes from (if they exist)
   * @return {@code null} if the {@link SchemaMetadata} has no {@link
   *     Constants#SETTING_SEMANTIC_PREFIXES} setting, otherwise the defined {@link Namespace}{@code
   *     s} (which can be empty if the setting has an empty value).
   */
  @Nullable
  private static Set<Namespace> getCustomPrefixes(SchemaMetadata schema) {
    if (!schema.getSettings().containsKey(SETTING_SEMANTIC_PREFIXES)) {
      return null;
    }

    Set<Namespace> namespaces = new HashSet<>();
    try (MappingIterator<Map<String, String>> iterator =
        new CsvMapper()
            .readerForMapOf(String.class)
            .with(SEMANTIC_PREFIXES_CSV_SCHEMA)
            .readValues(schema.getSetting(SETTING_SEMANTIC_PREFIXES))) {

      iterator.forEachRemaining(
          row -> {
            namespaces.add(
                validateNamespace(
                    schema,
                    Values.namespace(
                        row.get(SEMANTIC_PREFIXES_NAME_PREFIX),
                        row.get(SEMANTIC_PREFIXES_NAME_IRI))));
          });
    } catch (IOException e) {
      logger.error("Failed to retrieve custom prefixes, falling back to default", e);
      return null;
    }
    return namespaces;
  }

  private static Namespace validateNamespace(SchemaMetadata schema, Namespace namespace) {
    try {
      Values.iri(namespace, "test");
    } catch (Exception e) {
      throw new MolgenisException(
          "Unusable IRI found in custom_prefixes of %s: %s,%s"
              .formatted(schema.getName(), namespace.getPrefix(), namespace.getName()));
    }
    return namespace;
  }

  private final Map<String, Namespace> namespaces;
  private final boolean defaultNamespaces;

  public boolean isDefaultNamespaces() {
    return defaultNamespaces;
  }

  /**
   * @param namespaces If {@code null}, will use the {@link
   *     SemanticPrefixes#DEFAULT_NAMESPACES_MAP}. If a {@link Collection} is given (either empty or
   *     with items), uses that instead.
   */
  public SemanticPrefixes(Collection<Namespace> namespaces) {
    if (namespaces == null) {
      defaultNamespaces = true;
      this.namespaces = DEFAULT_NAMESPACES_MAP;
      return;
    }

    defaultNamespaces = false;
    this.namespaces =
        namespaces.stream()
            .map(namespace -> Map.entry(namespace.getPrefix(), namespace))
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (namespace1, namespace2) -> {
                      throw new MolgenisException(
                          "Prefix already exists: " + namespace1.getPrefix());
                    },
                    HashMap::new));
  }

  public SemanticPrefixes(SchemaMetadata schemaMetadata) {
    this(getCustomPrefixes(schemaMetadata));
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
  private <R> R map(
      final Semantic semantic,
      final Function<String, R> iriOperator,
      final Function<String, R> prefixedNameOperator) {
    return semantic.get().startsWith("<")
        ? iriOperator.apply(semantic.get().substring(1, semantic.get().length() - 1))
        : prefixedNameOperator.apply(semantic.get());
  }

  private Namespace getNamespace(final String prefix) {
    Namespace foundNamespace = namespaces.get(prefix);
    if (foundNamespace == null)
      throw new MolgenisException("Semantic uses an undefined prefix label: " + prefix);
    return foundNamespace;
  }

  /** Maps a semantic to a list of {@link IRI}{@code s} that represent a sequence path. */
  public IRI mapAsIri(final Semantic semantic) {
    return map(
        semantic,
        Values::iri,
        prefixedName -> {
          String[] split = prefixedName.split(":");
          return Values.iri(getNamespace(split[0]), split[1]);
        });
  }

  /** Maps a semantic to a list of {@link String}{@code s} that represent a sequence path. */
  public String mapAsString(final Semantic semantic) {
    return map(
        semantic,
        "<%s>"::formatted,
        prefixedName -> {
          getNamespace(prefixedName.split(":")[0]);
          return prefixedName;
        });
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    SemanticPrefixes that = (SemanticPrefixes) o;
    return Objects.equals(namespaces, that.namespaces);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(namespaces);
  }
}
