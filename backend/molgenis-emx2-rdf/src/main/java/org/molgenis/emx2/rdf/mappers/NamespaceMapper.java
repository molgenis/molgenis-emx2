package org.molgenis.emx2.rdf.mappers;

import static org.molgenis.emx2.rdf.RdfUtils.SETTING_SEMANTIC_PREFIXES;
import static org.molgenis.emx2.rdf.RdfUtils.getSchemaNamespace;
import static org.molgenis.emx2.rdf.RdfUtils.hasIllegalPrefix;
import static org.molgenis.emx2.rdf.RdfUtils.isIllegalIri;
import static org.molgenis.emx2.rdf.RdfUtils.isIllegalPrefix;
import static org.molgenis.emx2.utils.URLUtils.validateUrl;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.util.Values;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.rdf.DefaultNamespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamespaceMapper {
  private static final Logger logger = LoggerFactory.getLogger(NamespaceMapper.class);

  private static final Map<String, Namespace> DEFAULT_NAMESPACES_MAP =
      DefaultNamespace.streamAll().collect(Collectors.toMap(Namespace::getPrefix, i -> i));

  private static final Set<Namespace> DEFAULT_NAMESPACES_SET =
      DefaultNamespace.streamAll().collect(Collectors.toUnmodifiableSet());

  private static final CsvSchema SEMANTIC_PREFIXES_CSV_SCHEMA =
      CsvSchema.builder().addColumn("prefix").addColumn("iri").build();

  private final String baseUrl;

  // We need to store the namespaces per schema to ensure each prefix is processed correctly
  // (as different schema's can have a different namespace URL for the same prefix and vice versa).
  // Outer map uses SortedMap to ensure schema's are always processed in the same order.
  // If the outer Map contains null as value, it means that the setting is not defined.
  // If the outer Map contains an empty Map, the setting was defined but empty.
  // schema name -> namespace prefix -> namespace
  private final SortedMap<String, Map<String, Namespace>> namespaces = new TreeMap<>();

  // Namespaces for the schema's themselves.
  private final Map<String, Namespace> schemaNamespaces = new HashMap<>();

  public NamespaceMapper(String baseUrl, Collection<Schema> schemas) {
    this.baseUrl = validateUrl(baseUrl);
    addAll(schemas);
  }

  public NamespaceMapper(String baseUrl, Schema schema) {
    this.baseUrl = validateUrl(baseUrl);
    add(schema);
  }

  public NamespaceMapper(String baseUrl) {
    this.baseUrl = validateUrl(baseUrl);
  }

  private void add(Schema schema) {
    namespaces.put(schema.getName(), getCustomPrefixes(schema));
    schemaNamespaces.put(schema.getMetadata().getIdentifier(), getSchemaNamespace(baseUrl, schema));
  }

  private void addAll(Collection<Schema> schemas) {
    for (Schema schema : schemas) add(schema);
  }

  public Set<Namespace> getAllNamespaces(Schema schema) {
    Set<Namespace> namespaceSet = new HashSet<>();
    Namespace schemaNamespace = schemaNamespaces.get(schema.getMetadata().getIdentifier());
    if (schemaNamespace == null) {
      throw new IllegalArgumentException(
          "Schema \"" + schema.getMetadata().getIdentifier() + "\" was not processed for mapping");
    }
    namespaceSet.add(schemaNamespace);

    Map<String, Namespace> customNamespaces = namespaces.get(schema.getName());
    if (customNamespaces == null) {
      namespaceSet.addAll(DEFAULT_NAMESPACES_SET);
    } else {
      namespaceSet.addAll(customNamespaces.values());
    }

    return namespaceSet;
  }

  /**
   * Alphabetical order of Schema names defines which prefix takes priority. Default namespaces has
   * highest priority, if used.
   */
  public Set<Namespace> getAllNamespaces() {
    SortedSet<Namespace> combinedNameSpaces = new TreeSet<>();

    Set<String> processedPrefixes = new HashSet<>();
    Set<String> processedNames = new HashSet<>();

    // if any Schema uses default namespaces, ensure these take priority!
    if (namespaces.containsValue(null)) {
      DEFAULT_NAMESPACES_MAP
          .values()
          .forEach(
              i -> {
                processedPrefixes.add(i.getPrefix());
                processedNames.add(i.getName());
                combinedNameSpaces.add(i);
              });
    }

    // Add namespaces specific for each schema.
    schemaNamespaces
        .values()
        .forEach(
            i -> {
              processedPrefixes.add(i.getPrefix());
              processedNames.add(i.getName());
              combinedNameSpaces.add(i);
            });

    // Add custom namespaces.
    namespaces.values().stream()
        .filter(Objects::nonNull)
        .map(Map::values)
        .flatMap(Collection::stream)
        .forEach(
            i -> {
              if (!processedPrefixes.contains(i.getPrefix())
                  && !processedNames.contains(i.getName())) {
                processedPrefixes.add(i.getPrefix());
                processedNames.add(i.getName());
                combinedNameSpaces.add(i);
              }
            });
    return combinedNameSpaces;
  }

  private static Map<String, Namespace> getCustomPrefixes(Schema schema) {
    if (!schema.hasSetting(SETTING_SEMANTIC_PREFIXES)) {
      return null;
    }

    Map<String, Namespace> namespaces = new HashMap<>();
    try (MappingIterator<Map<String, String>> iterator =
        new CsvMapper()
            .readerForMapOf(String.class)
            .with(SEMANTIC_PREFIXES_CSV_SCHEMA)
            .readValues(schema.getSettingValue(SETTING_SEMANTIC_PREFIXES))) {
      iterator.forEachRemaining(
          i -> {
            if (isIllegalPrefix(i.get("prefix"))) {
              throw new MolgenisException(
                  "Schema \""
                      + schema.getName()
                      + "\" contains a prefix that is not allowed: "
                      + i.get("prefix"));
            }
            if (isIllegalIri(i.get("iri"))) {
              throw new MolgenisException(i.get("iri") + " must be a valid (absolute) IRI");
            }
            namespaces.put(i.get("prefix"), Values.namespace(i.get("prefix"), i.get("iri")));
          });
    } catch (IOException e) {
      // If retrieval fails, use default namespaces instead.
      logger.error(
          "An error occurred while trying to process the custom namespaces (using no namespaces instead): "
              + e.getMessage());
    }

    return namespaces;
  }

  public IRI map(final SchemaMetadata schema, final String semantic) {
    if (!namespaces.containsKey(schema.getName())) {
      throw new IllegalArgumentException(
          "Schema \"" + schema.getName() + "\" was not processed for mapping");
    }

    Map<String, Namespace> namespacesToSearch = namespaces.get(schema.getName());
    if (namespacesToSearch == null) namespacesToSearch = DEFAULT_NAMESPACES_MAP;

    String[] semanticSplit = semantic.split(":", 2);
    if (semanticSplit.length == 1) { // If @base is supported, might need to be changed.
      throw new IllegalArgumentException("Invalid semantics (missing \":\"): " + semantic);
    }

    // If used prefix is schema id -> use schema namespace.
    if (semanticSplit[0].equals(schema.getIdentifier())) {
      return Values.iri(schemaNamespaces.get(schema.getIdentifier()), semanticSplit[1]);
    }

    // Search through schema-specific namespaces.
    Namespace foundNamespace = namespacesToSearch.get(semanticSplit[0]);
    if (foundNamespace == null) {
      if (logger.isDebugEnabled() && !hasIllegalPrefix(semantic)) {
        logger.debug("Found undefined prefix (unless IRI is expected): \"" + semantic + "\"");
      }
      return Values.iri(semantic);
    }
    return Values.iri(foundNamespace, semanticSplit[1]);
  }

  public IRI map(final Schema schema, final String semantic) {
    return map(schema.getMetadata(), semantic);
  }

  @Override
  public String toString() {
    return "NamespaceMapper{" + "namespaces=" + namespaces + '}';
  }
}
