package org.molgenis.emx2.semantics;

import static org.molgenis.emx2.semantics.rdf.ColumnToRDF.describeColumns;
import static org.molgenis.emx2.semantics.rdf.IRIParsingEncoding.getURI;
import static org.molgenis.emx2.semantics.rdf.RootToRDF.describeRoot;
import static org.molgenis.emx2.semantics.rdf.SchemaToRDF.describeSchema;
import static org.molgenis.emx2.semantics.rdf.SupportedRDFFileFormats.RDF_FILE_FORMATS;
import static org.molgenis.emx2.semantics.rdf.TableToRDF.describeTable;
import static org.molgenis.emx2.semantics.rdf.ValueToRDF.describeValues;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.molgenis.emx2.*;

// TODO check null value handling
// TODO check value types
// TODO make sure no classes are used as predicates and vice versa
// TODO: ontology tables need semantics to denote "what are these rows instances of?" (typeOf in FG)
// TODO: units for values?

/**
 * Nomenclature used from:
 *
 * <ul>
 *   <li>SIO (http://semanticscience.org)
 *   <li>RDF Data Cube (https://www.w3.org/TR/vocab-data-cube)
 *   <li>OWL, RDF, RDFS
 * </ul>
 */
public class RDFService {

  private ObjectMapper jsonMapper;
  private ModelBuilder builder;
  private WriterConfig config;
  private String host;

  /**
   * Hidden constructor, used on-the-fly by static functions that handle requests.
   *
   * @param requestURL
   * @param response
   */
  public RDFService(String requestURL) {

    // reconstruct server:port URL to prevent problems with double encoding of schema/table names
    // etc
    URI requestURI = getURI(requestURL);
    this.host = extractHost(requestURI);

    jsonMapper =
        new ObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setDateFormat(new StdDateFormat().withColonInTimeZone(true));

    this.builder = new ModelBuilder();
    this.config = new WriterConfig();
    this.config.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
    this.builder.setNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
    this.builder.setNamespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
    this.builder.setNamespace("xsd", "http://www.w3.org/2001/XMLSchema#");
    this.builder.setNamespace("owl", "http://www.w3.org/2002/07/owl#");
    this.builder.setNamespace("sio", "http://semanticscience.org/resource/");
    this.builder.setNamespace("qb", "http://purl.org/linked-data/cube#");
    this.builder.setNamespace("dcterms", "http://purl.org/dc/terms/");
  }

  /**
   * Retrieve EMX2 data described as RDF. Can be used in different ways:
   *
   * <ul>
   *   <li>Call with one or more schemas, table and rowId null: retrieve all data from selected
   *       schemas
   *   <li>Call with a table, schema of that table, rowId null: retrieve all data from selected
   *       table
   *   <li>Call with a table, schema of that table, rowId provided: retrieve all data from selected
   *       row
   * </ul>
   *
   * <p>Each call will result in a full stack of data, containing the following elements:
   *
   * <ul>
   *   <li>Root node with server URL
   *   <li>Schema node(s) linked to its root
   *   <li>Table node(s) linked to its schema
   *   <li>Column node(s) linked to its table
   *   <li>Row node(s) linked to its table with value(s) linked to its column(s)
   * </ul>
   */
  public void describeAsRDF(
      OutputStream outputStream,
      String rdfApiLocation,
      String format,
      Table table,
      String rowId,
      String columnName,
      Schema... schemas) {
    try {

      describeRoot(builder, host);

      for (int i = 0; i < schemas.length; i++) {
        Schema schema = schemas[i];
        String schemaRdfApiContext = host + "/" + schema.getName() + rdfApiLocation;
        builder.setNamespace("emx" + i, schemaRdfApiContext + "/");
        describeSchema(builder, schema, schemaRdfApiContext, host);
        List<Table> tables = table != null ? Arrays.asList(table) : schema.getTablesSorted();
        for (Table tableToDescribe : tables) {
          describeTable(builder, tableToDescribe, schemaRdfApiContext);
          describeColumns(builder, columnName, tableToDescribe, schemaRdfApiContext);
          // if a column name is provided then only provide column metadata, no row values
          if (columnName == null) {
            describeValues(builder, tableToDescribe, rowId, schemaRdfApiContext);
          }
        }
      }

      Rio.write(builder.build(), outputStream, getFormat(format), config);

    } catch (Exception e) {
      throw new MolgenisException("RDF export failed due to an exception", e);
    }
  }

  /**
   * Extract the host location from a request URI.
   *
   * @param requestURI
   * @return
   */
  public static String extractHost(URI requestURI) {
    return requestURI.getScheme()
        + "://"
        + requestURI.getHost()
        + (requestURI.getPort() != -1 ? ":" + requestURI.getPort() : "");
  }

  private ObjectMapper getJsonMapper() {
    return jsonMapper;
  }

  public ModelBuilder getBuilder() {
    return builder;
  }

  public WriterConfig getConfig() {
    return config;
  }

  public String getHost() {
    return host;
  }

  public RDFFormat getFormat(String format) {
    RDFFormat rdfFormat;
    if (format == null) {
      rdfFormat = RDFFormat.TURTLE;
    } else {
      if (!RDF_FILE_FORMATS.keySet().contains(format)) {
        throw new MolgenisException("Format unknown. Use any of: " + RDF_FILE_FORMATS.keySet());
      }
      rdfFormat = RDF_FILE_FORMATS.get(format);
    }
    return rdfFormat;
  }

  public String getMimeType(String format) {
    return getFormat(format).getDefaultMIMEType();
  }
}
