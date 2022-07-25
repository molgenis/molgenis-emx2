package org.molgenis.emx2.semantics;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.molgenis.emx2.semantics.rdf.ColumnToRDF.describeColumns;
import static org.molgenis.emx2.semantics.rdf.SchemaToRDF.describeSchema;
import static org.molgenis.emx2.semantics.rdf.SupportedRDFFileFormats.RDF_FILE_FORMATS;
import static org.molgenis.emx2.semantics.rdf.TableToRDF.describeTable;
import static org.molgenis.emx2.semantics.rdf.ValueToRDF.describeValues;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import java.io.PrintWriter;
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

  /**
   * Hidden constructor, used on-the-fly by static functions that handle requests.
   *
   * @param request
   * @param response
   */
  private RDFService(Request request, Response response) throws Exception {

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
      Table table, PrintWriter writer, Request request, Response response) {
    try {

      RDFService rdfService = new RDFService(request, response);
      IRI schemaContext =
          iri(request.url().substring(0, request.url().length() - table.getName().length() - 1));
      rdfService.getBuilder().setNamespace("emx", schemaContext.stringValue() + "/");

      describeSchema(rdfService.getBuilder(), table.getSchema(), schemaContext);
      describeTable(rdfService.getBuilder(), table, schemaContext);
      describeColumns(rdfService.getBuilder(), table, schemaContext);
      describeValues(rdfService.getJsonMapper(), rdfService.getBuilder(), table, schemaContext);

      Rio.write(
          rdfService.getBuilder().build(),
          writer,
          rdfService.getRdfFormat(),
          rdfService.getConfig());

    } catch (Exception e) {
      throw new MolgenisException("RDF export failed due to an exception.", e);
    }
  }

  /**
   * Output is an RDF definition of the schema, its tables, and the columns of these tables. So no
   * data.
   *
   * @param schema
   * @param writer
   * @param request
   * @param response
   */
  public static void getRdfForSchema(
      Schema schema, PrintWriter writer, Request request, Response response) {
    try {

      RDFService rdfService = new RDFService(request, response);
      IRI schemaContext = iri(request.url());
      rdfService.getBuilder().setNamespace("emx", schemaContext.stringValue() + "/");

      describeSchema(rdfService.getBuilder(), schema, schemaContext);
      for (Table t : schema.getTablesSorted()) {
        describeTable(rdfService.getBuilder(), t, schemaContext);
      }
      for (Table t : schema.getTablesSorted()) {
        describeColumns(rdfService.getBuilder(), t, schemaContext);
      }

      Rio.write(
          rdfService.getBuilder().build(),
          writer,
          rdfService.getRdfFormat(),
          rdfService.getConfig());

    } catch (Exception e) {
      throw new MolgenisException("RDF export failed due to an exception.", e);
    }
  }

  /**
   * Output is an RDF definition of all database schemas, all of their tables, as well as all table
   * columns and row values. In other words: a complete database dump, depending on authorization.
   *
   * @param database
   * @param printWriter
   * @param request
   * @param response
   */
  public static void getRdfDatabaseDump(
      Database database, PrintWriter printWriter, Request request, Response response) {
    for (String schemaName : database.getSchemaNames()) {
      Schema schema = database.getSchema(schemaName);
    }
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
}
