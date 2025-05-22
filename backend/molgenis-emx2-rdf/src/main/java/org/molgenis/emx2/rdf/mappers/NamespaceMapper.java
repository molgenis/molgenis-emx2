package org.molgenis.emx2.rdf.mappers;

import static org.molgenis.emx2.rdf.DefaultNamespace.streamAll;
import static org.molgenis.emx2.rdf.RdfUtils.SETTING_SEMANTIC_PREFIXES;
import static org.molgenis.emx2.rdf.RdfUtils.hasIllegalPrefix;
import static org.molgenis.emx2.rdf.RdfUtils.isIllegalPrefix;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.util.Values;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamespaceMapper {
  private static final Logger logger = LoggerFactory.getLogger(NamespaceMapper.class);

  private static final Map<String, Namespace> DEFAULT_NAMESPACES_MAP =
      streamAll().collect(Collectors.toMap(Namespace::getPrefix, i -> i));

  private static final CsvSchema SEMANTIC_PREFIXES_CSV_SCHEMA =
      CsvSchema.builder().addColumn("prefix").addColumn("iri").build();

  // We need to store the namespaces per schema to ensure each prefix is processed correctly
  // (as different schema's can have a different namespace URL for the same prefix).
  // Uses sorted map to ensure conflicts are handled identically.
  // schema -> namespace prefix -> namespace
  private final SortedMap<String, Map<String, Namespace>> namespaces = new TreeMap<>();

  public NamespaceMapper(Collection<Schema> schemas) throws IOException {
    addAll(schemas);
  }

  public NamespaceMapper(Schema schema) throws IOException {
    add(schema);
  }

  public NamespaceMapper() {}

  private void add(Schema schema) throws IOException {
    Map<String, Namespace> schemaNamespaces = getCustomPrefixes(schema);
    namespaces.put(schema.getName(), schemaNamespaces);
  }

  private void addAll(Collection<Schema> schemas) throws IOException {
    for (Schema schema : schemas) add(schema);
  }

  /**
   * Alphabetical order of Schema names defines which prefix takes priority. Default namespaces has
   * highest priority, if used.
   */
  public Set<Namespace> getAllNamespaces() {
    Set<Namespace> combinedNameSpaces = new HashSet<>();

    // if any Schema uses default namespaces, ensure these take priority!
    if (namespaces.containsValue(null)) {
      combinedNameSpaces.addAll(DEFAULT_NAMESPACES_MAP.values());
    }

    namespaces.values().stream()
        .map(Map::values)
        .flatMap(Collection::stream)
        .forEach(combinedNameSpaces::add);
    return combinedNameSpaces;
  }

  private static Map<String, Namespace> getCustomPrefixes(Schema schema) throws IOException {
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
            // Check similar to RDF4J's SimpleIRI
            if (i.get("iri").indexOf(':') < 0) {
              throw new MolgenisException(i.get("iri") + " must be a valid (absolute) IRI");
            }
            namespaces.put(i.get("prefix"), Values.namespace(i.get("prefix"), i.get("iri")));
          });
    }

    return namespaces;
  }

  private IRI map(final String schemaName, final String semantic) {
    if (!namespaces.containsKey(schemaName)) {
      throw new MolgenisException("Schema \"" + schemaName + "\" is not defined");
    }

    Map<String, Namespace> schemaNamespaces = namespaces.get(schemaName);
    if (schemaNamespaces == null) schemaNamespaces = DEFAULT_NAMESPACES_MAP;

    String[] semanticSplit = semantic.split(":", 2);
    if (semanticSplit.length == 1) { // If @base is supported, might need to be changed.
      throw new MolgenisException("Invalid semantics (missing \":\"): " + semantic);
    }
    Namespace foundNamespace = schemaNamespaces.get(semanticSplit[0]);
    if (foundNamespace == null) {
      if (logger.isDebugEnabled() && !hasIllegalPrefix(semantic)) {
        logger.debug("Found undefined prefix (unless IRI is expected): \"" + semantic + "\"");
      }
      return Values.iri(semantic);
    }
    return Values.iri(foundNamespace, semanticSplit[1]);
  }

  public IRI map(final Schema schema, final String semantic) {
    return map(schema.getName(), semantic);
  }

  public IRI map(final SchemaMetadata schema, final String semantic) {
    return map(schema.getName(), semantic);
  }

  @Override
  public String toString() {
    return "NamespaceMapper{" + "namespaces=" + namespaces + '}';
  }
}
