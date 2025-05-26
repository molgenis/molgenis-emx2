package org.molgenis.emx2.rdf;

import static org.molgenis.emx2.rdf.DefaultNamespace.streamAll;
import static org.molgenis.emx2.rdf.IriGenerator.schemaIRI;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class RdfUtils {
  private static final Logger logger = LoggerFactory.getLogger(RdfUtils.class);

  // Advanced setting containing valid Turtle-formatted RDF.
  public static final String SETTING_CUSTOM_RDF = "custom_rdf";
  public static final String SETTING_SEMANTIC_PREFIXES = "semantic_prefixes";

  public static final Map<String, Namespace> DEFAULT_NAMESPACES_MAP =
      streamAll().collect(Collectors.toMap(Namespace::getPrefix, i -> i));

  public static final CsvSchema SEMANTIC_PREFIXES_CSV_SCHEMA =
      CsvSchema.builder().addColumn("prefix").addColumn("iri").build();
  // Matches with Strings like "urn:" & "urn:uuid:", but allows variations such as "urnamespace:"
  public static final Pattern ILLEGAL_PREFIX = Pattern.compile("^(http(s)?|urn(:.*)?|tag):");

  /**
   * Get the namespace for a schema. A namespace URL does have a trailing slash (as it is used for
   * defining relative paths).
   *
   * @param baseURL the baseURL that needs to be used
   * @param schema the schema
   * @return A namespace that defines a local unique prefix for this schema.
   */
  static Namespace getSchemaNamespace(final String baseURL, final SchemaMetadata schema) {
    final String prefix = schema.getIdentifier();
    final String url = schemaIRI(baseURL, schema).stringValue() + "/";
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
    String[] semanticSplit = semantic.split(":", 2);
    if (semanticSplit.length == 1) { // If @base is supported, might need to be changed.
      throw new MolgenisException("Invalid semantics (missing \":\"): " + semantic);
    }
    Namespace foundNamespace = namespaces.get(semanticSplit[0]);
    if (foundNamespace == null) {
      if (logger.isDebugEnabled() && !hasIllegalPrefix(semantic)) {
        logger.debug("Found undefined prefix (unless IRI is expected): \"" + semantic + "\"");
      }
      return Values.iri(semantic);
    }
    return Values.iri(foundNamespace, semanticSplit[1]);
  }

  public static boolean hasIllegalPrefix(String semantic) {
    return ILLEGAL_PREFIX.matcher(semantic).find();
  }

  public static boolean isIllegalPrefix(String prefix) {
    return hasIllegalPrefix(prefix + ':');
  }

  /** Ensure that the base URL has no trailing "/". */
  public static String formatBaseURL(String baseURL) {
    String baseUrlTrim = baseURL.trim();
    return baseUrlTrim.endsWith("/")
        ? baseUrlTrim.substring(0, baseUrlTrim.length() - 1)
        : baseUrlTrim;
  }
}
