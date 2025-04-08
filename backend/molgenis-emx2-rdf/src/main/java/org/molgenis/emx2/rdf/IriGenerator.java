package org.molgenis.emx2.rdf;

import static com.google.common.net.UrlEscapers.urlPathSegmentEscaper;
import static org.molgenis.emx2.Constants.API_FILE;
import static org.molgenis.emx2.Constants.API_RDF;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.TableMetadata;

public class IriGenerator {
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
            + urlPathSegmentEscaper().escape(table.getSchemaName())
            + API_RDF
            + "/"
            + urlPathSegmentEscaper().escape(table.getIdentifier()));
  }

  static IRI tableIRI(String baseURL, Table table) {
    return tableIRI(baseURL, table.getMetadata());
  }

  static IRI columnIRI(String baseURL, Column column) {
    return Values.iri(
        baseURL
            + "/"
            + urlPathSegmentEscaper().escape(column.getSchemaName())
            + API_RDF
            + "/"
            + urlPathSegmentEscaper().escape(column.getTable().getIdentifier())
            + "/column/"
            + urlPathSegmentEscaper().escape(column.getIdentifier()));
  }

  static IRI rowIRI(String baseURL, TableMetadata table, PrimaryKey primaryKey) {
    return Values.iri(
        baseURL
            + "/"
            + urlPathSegmentEscaper().escape(table.getSchemaName())
            + API_RDF
            + "/"
            + urlPathSegmentEscaper().escape(table.getIdentifier())
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
            + urlPathSegmentEscaper().escape(column.getSchemaName())
            + API_FILE
            + "/"
            + urlPathSegmentEscaper().escape(column.getTable().getIdentifier())
            + "/"
            + urlPathSegmentEscaper().escape(column.getName())
            + "/"
            + urlPathSegmentEscaper().escape(row.getString(column.getName())));
  }
}
