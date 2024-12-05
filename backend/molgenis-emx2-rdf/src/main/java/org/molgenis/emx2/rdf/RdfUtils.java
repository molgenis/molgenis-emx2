package org.molgenis.emx2.rdf;

import com.google.common.net.UrlEscapers;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.util.Values;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.utils.TypeUtils;

abstract class RdfUtils {
  /**
   * Get the namespace for a schema
   *
   * @param baseURL the baseURL that needs to be used
   * @param schema the schema
   * @return A namespace that defines a local unique prefix for this schema.
   */
  static Namespace getSchemaNamespace(final String baseURL, final SchemaMetadata schema) {
    final String schemaName = UrlEscapers.urlPathSegmentEscaper().escape(schema.getName());
    final String url = baseURL + schemaName + "/api/rdf/";
    final String prefix = TypeUtils.convertToPascalCase(schema.getName());
    return Values.namespace(prefix, url);
  }

  static Namespace getSchemaNamespace(final String baseURL, final Schema schema) {
    return getSchemaNamespace(baseURL, schema.getMetadata());
  }
}
