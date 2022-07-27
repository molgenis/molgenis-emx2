package org.molgenis.emx2.semantics;

import static org.molgenis.emx2.semantics.rdf.ColumnToRDF.describeColumns;
import static org.molgenis.emx2.semantics.rdf.RootToRDF.describeRoot;
import static org.molgenis.emx2.semantics.rdf.SchemaToRDF.describeSchema;
import static org.molgenis.emx2.semantics.rdf.SupportedRDFFileFormats.RDF_FILE_FORMATS;
import static org.molgenis.emx2.semantics.rdf.TableToRDF.describeTable;
import static org.molgenis.emx2.semantics.rdf.ValueToRDF.describeValues;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.molgenis.emx2.*;
import spark.Request;
import spark.Response;

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
  private RDFFormat rdfFormat;
  private String rootContext;

  /**
   * Hidden constructor, used on-the-fly by static functions that handle requests.
   *
   * @param request
   * @param response
   */
  private RDFService(Request request, Response response) throws Exception {

    // reconstruct server:port URL to prevent problems with double encoding of schema/table names
    // etc
    String requestURL = request.url();
    URI requestURI = getURI(requestURL);
    this.rootContext =
        requestURI.getScheme() + "://" + requestURI.getHost() + ":" + requestURI.getPort();

    jsonMapper =
        new ObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setDateFormat(new StdDateFormat().withColonInTimeZone(true));

    if (request.queryParams("format") == null) {
      this.rdfFormat = RDFFormat.TURTLE;
    } else {
      String format = request.queryParams("format");
      if (!RDF_FILE_FORMATS.keySet().contains(format)) {
        throw new Exception("Format unknown. Use any of: " + RDF_FILE_FORMATS.keySet());
      }
      this.rdfFormat = RDF_FILE_FORMATS.get(format);
    }
    response.type(this.rdfFormat.getDefaultMIMEType());

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
   * Output is an RDF definition of the schema, the selected table, the columns of this table, and
   * all values contained within its rows.
   *
   * @param table
   * @param writer
   * @param request
   * @param response
   */
  public static void getRdfForTable(
      Table table,
      String rowId,
      PrintWriter writer,
      Request request,
      Response response,
      String rdfApiLocation) {
    try {

      RDFService rdfService = new RDFService(request, response);
      describeRoot(rdfService.getBuilder(), rdfService.getRootContext());

      String schemaRdfApiContext =
          rdfService.getRootContext() + "/" + table.getSchema().getName() + rdfApiLocation;
      rdfService.getBuilder().setNamespace("emx", schemaRdfApiContext + "/");

      describeSchema(
          rdfService.getBuilder(),
          table.getSchema(),
          schemaRdfApiContext,
          rdfService.getRootContext());
      describeTable(rdfService.getBuilder(), table, schemaRdfApiContext);
      describeColumns(rdfService.getBuilder(), table, schemaRdfApiContext);
      describeValues(
          rdfService.getJsonMapper(), rdfService.getBuilder(), table, rowId, schemaRdfApiContext);

      Rio.write(
          rdfService.getBuilder().build(),
          writer,
          rdfService.getRdfFormat(),
          rdfService.getConfig());

    } catch (Exception e) {
      throw new MolgenisException("RDF export failed due to an exception", e);
    }
  }

  /**
   * Output is an RDF definition of the schema, its tables, the columns of this table, and all
   * values contained within its rows.
   *
   * @param schema
   * @param writer
   * @param request
   * @param response
   */
  public static void getRdfForSchema(
      Schema schema,
      PrintWriter writer,
      Request request,
      Response response,
      String rdfApiLocation) {
    try {

      RDFService rdfService = new RDFService(request, response);
      describeRoot(rdfService.getBuilder(), rdfService.getRootContext());

      String schemaRdfApiContext =
          rdfService.getRootContext() + "/" + schema.getName() + rdfApiLocation;
      rdfService.getBuilder().setNamespace("emx", schemaRdfApiContext + "/");

      describeSchema(
          rdfService.getBuilder(), schema, schemaRdfApiContext, rdfService.getRootContext());
      for (Table table : schema.getTablesSorted()) {
        describeTable(rdfService.getBuilder(), table, schemaRdfApiContext);
        describeColumns(rdfService.getBuilder(), table, schemaRdfApiContext);
        describeValues(
            rdfService.getJsonMapper(), rdfService.getBuilder(), table, null, schemaRdfApiContext);
      }

      Rio.write(
          rdfService.getBuilder().build(),
          writer,
          rdfService.getRdfFormat(),
          rdfService.getConfig());

    } catch (Exception e) {
      throw new MolgenisException("RDF export failed due to an exception", e);
    }
  }

  /**
   * Output is an RDF definition of all database schemas, all of their tables, as well as all table
   * columns and row values. In other words: a complete database dump, depending on authorization.
   *
   * @param schemas
   * @param writer
   * @param request
   * @param response
   */
  public static void getRdfForDatabase(
      List<Schema> schemas,
      PrintWriter writer,
      Request request,
      Response response,
      String rdfApiLocation) {
    try {

      RDFService rdfService = new RDFService(request, response);
      describeRoot(rdfService.getBuilder(), rdfService.getRootContext());

      for (int i = 0; i < schemas.size(); i++) {
        Schema schema = schemas.get(i);
        String schemaRdfApiContext =
            rdfService.getRootContext() + "/" + schema.getName() + rdfApiLocation;
        rdfService.getBuilder().setNamespace("emx" + i, schemaRdfApiContext + "/");
        describeSchema(
            rdfService.getBuilder(), schema, schemaRdfApiContext, rdfService.getRootContext());
        for (Table table : schema.getTablesSorted()) {
          describeTable(rdfService.getBuilder(), table, schemaRdfApiContext);
          describeColumns(rdfService.getBuilder(), table, schemaRdfApiContext);
          describeValues(
              rdfService.getJsonMapper(),
              rdfService.getBuilder(),
              table,
              null,
              schemaRdfApiContext);
        }
      }

      Rio.write(
          rdfService.getBuilder().build(),
          writer,
          rdfService.getRdfFormat(),
          rdfService.getConfig());

    } catch (Exception e) {
      throw new MolgenisException("RDF export failed due to an exception", e);
    }
  }

  /**
   * @param uriString
   * @return
   * @throws URISyntaxException
   */
  public static URI getURI(String uriString) throws URISyntaxException {
    ParsedIRI parsedIRI = ParsedIRI.create(uriString);
    URI uri =
        new URI(
            parsedIRI.getScheme(),
            parsedIRI.getUserInfo(),
            parsedIRI.getHost(),
            parsedIRI.getPort(),
            parsedIRI.getPath(),
            parsedIRI.getQuery(),
            parsedIRI.getFragment());
    return uri;
  }

  /**
   * @param uriString
   * @return
   */
  public static IRI encodedIRI(String uriString) {
    return org.eclipse.rdf4j.model.util.Values.iri(ParsedIRI.create(uriString).toString());
  }

  private ObjectMapper getJsonMapper() {
    return jsonMapper;
  }

  private ModelBuilder getBuilder() {
    return builder;
  }

  private WriterConfig getConfig() {
    return config;
  }

  private RDFFormat getRdfFormat() {
    return rdfFormat;
  }

  private String getRootContext() {
    return rootContext;
  }
}
