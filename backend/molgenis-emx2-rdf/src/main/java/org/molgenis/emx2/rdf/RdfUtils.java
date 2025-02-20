package org.molgenis.emx2.rdf;

import static org.molgenis.emx2.Constants.API_RDF;
import static org.molgenis.emx2.rdf.DefaultNamespace.streamAll;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.common.net.UrlEscapers;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.utils.TypeUtils;

abstract class RdfUtils {
  // Advanced setting containing valid Turtle-formatted RDF.
  public static final String SETTING_CUSTOM_RDF = "custom_rdf";
  public static final String SETTING_SEMANTIC_PREFIXES = "semantic_prefixes";

  // Used to compare semantic field to define if it contains an IRI or a prefixed name
  private static final char SEMANTIC_IRI_STARTSWITH = '<';

  public static final Map<String, Namespace> DEFAULT_NAMESPACES_MAP =
      streamAll().collect(Collectors.toMap(Namespace::getPrefix, i -> i));

  public static final CsvSchema SEMANTIC_PREFIXES_CSV_SCHEMA =
      CsvSchema.builder().addColumn("prefix").addColumn("iri").build();

  /**
   * Get the namespace for a schema
   *
   * @param baseURL the baseURL that needs to be used
   * @param schema the schema
   * @return A namespace that defines a local unique prefix for this schema.
   */
  static Namespace getSchemaNamespace(final String baseURL, final SchemaMetadata schema) {
    final String schemaName = UrlEscapers.urlPathSegmentEscaper().escape(schema.getName());
    final String url = baseURL + schemaName + API_RDF + "/";
    final String prefix = TypeUtils.convertToPascalCase(schema.getName());
    return Values.namespace(prefix, url);
  }

  static Namespace getSchemaNamespace(final String baseURL, final Schema schema) {
    return getSchemaNamespace(baseURL, schema.getMetadata());
  }

  static Model getCustomRdf(Schema schema) throws IOException {
    if (schema.hasSetting(SETTING_CUSTOM_RDF)) {
      return Rio.parse(
          IOUtils.toInputStream(schema.getSettingValue(SETTING_CUSTOM_RDF), StandardCharsets.UTF_8),
          RDFFormat.TURTLE);
    }
    return null;
  }

  static Map<String, Namespace> getCustomPrefixes(Schema schema) throws IOException {
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
            // Check similar to RDF4J's SimpleIRI
            if (i.get("iri").indexOf(':') < 0) {
              throw new MolgenisException(i.get("iri") + " must be a valid (absolute) IRI");
            }
            namespaces.put(i.get("prefix"), Values.namespace(i.get("prefix"), i.get("iri")));
          });
    }

    return namespaces;
  }

  static Map<String, Namespace> getCustomPrefixesOrDefault(Schema schema) throws IOException {
    Map<String, Namespace> namespaces = getCustomPrefixes(schema);
    return (namespaces == null ? DEFAULT_NAMESPACES_MAP : namespaces);
  }

  /**
   * @param namespaces Schema name -> Namespace prefix -> Namespace
   */
  static IRI getSemanticValue(
      TableMetadata table, final Map<String, Map<String, Namespace>> namespaces, String semantic) {
    return getSemanticValue(namespaces.get(table.getSchema().getName()), semantic);
  }

  /**
   * @param namespaces Namespace prefix -> Namespace
   */
  static IRI getSemanticValue(final Map<String, Namespace> namespaces, String semantic) {
    if (semantic.charAt(0) == SEMANTIC_IRI_STARTSWITH) {
      return Values.iri(semantic.substring(1, semantic.length() - 1));
    }
    String[] semanticSplit = semantic.split(":", 2);
    Namespace foundNamespace = namespaces.get(semanticSplit[0]);
    if (foundNamespace == null) {
      throw new MolgenisException(
          "Could not find the prefix label \""
              + semanticSplit[0]
              + "\" within the given namespaces.");
    }
    return Values.iri(foundNamespace, semanticSplit[1]);
  }
}
