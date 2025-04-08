package org.molgenis.emx2.rdf;

import static com.google.common.net.UrlEscapers.urlPathSegmentEscaper;
import static org.molgenis.emx2.Constants.API_FILE;
import static org.molgenis.emx2.Constants.API_RDF;

import com.google.common.net.PercentEscaper;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.TableMetadata;

/**
 * Generates an IRI for Molgenis EMX2 objects.
 *
 * <h4>Note regarding escaping:</h4>
 *
 * For values expressed within the IRI, any reserved character according to <a
 * href=https://datatracker.ietf.org/doc/html/rfc3986#section-2.2>rfc3986</a> are escaped.
 * Therefore, the escaped result is similar to {@link
 * com.google.common.net.UrlEscapers#urlFormParameterEscaper}, except spaces are escaped as "{@code
 * %20}" instead of "{@code +}". Any reserved characters present in the IRI are therefore deliberate
 * and are not part of any user-data stored within EMX2.
 */
public class IriGenerator {
  static final PercentEscaper escaper = new PercentEscaper("-._~", false);

  public static IRI schemaIRI(String baseURL, SchemaMetadata schema) {
    return Values.iri(baseURL + "/" + urlPathSegmentEscaper().escape(schema.getName()) + API_RDF);
  }

  public static IRI schemaIRI(String baseURL, Schema schema) {
    return schemaIRI(baseURL, schema.getMetadata());
  }

  public static IRI tableIRI(String baseURL, TableMetadata table) {
    return Values.iri(
        baseURL
            + "/"
            + escaper.escape(table.getSchemaName())
            + API_RDF
            + "/"
            + escaper.escape(table.getIdentifier()));
  }

  static IRI tableIRI(String baseURL, Table table) {
    return tableIRI(baseURL, table.getMetadata());
  }

  static IRI columnIRI(String baseURL, Column column) {
    return Values.iri(
        baseURL
            + "/"
            + escaper.escape(column.getSchemaName())
            + API_RDF
            + "/"
            + escaper.escape(column.getTable().getIdentifier())
            + "/column/"
            + escaper.escape(column.getIdentifier()));
  }

  static IRI rowIRI(String baseURL, TableMetadata table, PrimaryKey primaryKey) {
    return Values.iri(
        baseURL
            + "/"
            + escaper.escape(table.getSchemaName())
            + API_RDF
            + "/"
            + escaper.escape(table.getIdentifier())
            + "/"
            + primaryKey.getEncodedValue());
  }

  static IRI rowIRI(String baseURL, Table table, PrimaryKey primaryKey) {
    return rowIRI(baseURL, table.getMetadata(), primaryKey);
  }

  static IRI fileIRI(String baseURL, Row row, Column column) {
    return Values.iri(
        baseURL
            + "/"
            + escaper.escape(column.getSchemaName())
            + API_FILE
            + "/"
            + escaper.escape(column.getTable().getIdentifier())
            + "/"
            + escaper.escape(column.getName())
            + "/"
            + escaper.escape(row.getString(column.getName())));
  }
}
