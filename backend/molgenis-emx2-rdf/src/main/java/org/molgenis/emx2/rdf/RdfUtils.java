package org.molgenis.emx2.rdf;

import static org.molgenis.emx2.rdf.IriGenerator.schemaIRI;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RdfUtils {
  private static final Logger logger = LoggerFactory.getLogger(RdfUtils.class);

  // Advanced setting containing valid Turtle-formatted RDF.
  public static final String SETTING_CUSTOM_RDF = "custom_rdf";

  // Advanced setting containing comma-separated "namespace prefix,namespace URL" pairs (per row)
  public static final String SETTING_SEMANTIC_PREFIXES = "semantic_prefixes";

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

  /**
   * @param semantic a prefixed name as defined <a href="https://www.w3.org/TR/turtle/#prefixed-name">here</a>
   */
  public static boolean hasIllegalPrefix(String semantic) {
    return ILLEGAL_PREFIX.matcher(semantic).find();
  }

  /**
   * @param prefix the prefix WITHOUT ':' or anything after that
   */
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

  /**
   * Check if IRI is valid similar to RDF4J's SimpleIRI
   * @see org.eclipse.rdf4j.model.impl.SimpleIRI
   */
  public static boolean isIllegalIri(String iri) {
    return (iri.indexOf(':') < 0);
  }
}
