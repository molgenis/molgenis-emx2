package org.molgenis.emx2.semantics;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.molgenis.emx2.semantics.rdf.ColumnToRDF.describeColumns;
import static org.molgenis.emx2.semantics.rdf.SupportedRDFFileFormats.RDF_FILE_FORMATS;
import static org.molgenis.emx2.semantics.rdf.SchemaToRDF.describeSchema;
import static org.molgenis.emx2.semantics.rdf.TableToRDF.describeTable;
import static org.molgenis.emx2.semantics.rdf.ValueToRDF.describeValues;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
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
  private static ObjectMapper jsonMapper =
      new ObjectMapper()
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .setDateFormat(new StdDateFormat().withColonInTimeZone(true));


  private RDFService() {
    // hidden
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
      if (request.queryParams("format") == null) {
        throw new Exception("Format not specified. Use any of: " + RDF_FILE_FORMATS.keySet());
      }
      String format = request.queryParams("format");
      if (!RDF_FILE_FORMATS.keySet().contains(format)) {
        throw new Exception("Format unknown. Use any of: " + RDF_FILE_FORMATS.keySet());
      }
      RDFFormat applicationOntologyFormat = RDF_FILE_FORMATS.get(format);
      response.type(applicationOntologyFormat.getDefaultMIMEType());

      IRI schemaContext =
          iri(request.url().substring(0, request.url().length() - table.getName().length() - 1));

      ModelBuilder builder = new ModelBuilder();
      ValueFactory vf = SimpleValueFactory.getInstance();
      WriterConfig config = new WriterConfig();
      config.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
      builder.setNamespace("sio", "http://semanticscience.org/resource/");
      builder.setNamespace("qb", "http://purl.org/linked-data/cube#");
      builder.setNamespace("xsd", "http://www.w3.org/2001/XMLSchema#");
      builder.setNamespace("emx", schemaContext.stringValue() + "/");

      describeSchema(builder, table.getSchema(), schemaContext);
      describeTable(builder, table, schemaContext);
      describeColumns(builder, table, schemaContext);
      describeValues(jsonMapper, builder, table, schemaContext);

      Model model = builder.build();
      StringWriter stringWriter = new StringWriter();
      Rio.write(model, stringWriter, applicationOntologyFormat, config);
      writer.append(stringWriter.toString());

    } catch (Exception e) {
      throw new MolgenisException("RDF export failed", e);
    }
  }

  /**
   * Output is an RDF definition of the schema, its tables, and the columns of these tables. So no
   * data.
   *
   * @param schema
   * @param printWriter
   * @param request
   * @param response
   */
  public static void getRdfForSchema(
      Schema schema, PrintWriter printWriter, Request request, Response response) {
    try {
      if (request.queryParams("format") == null) {
        throw new Exception(
            "Format not specified (using ?format=x). Use any of: " + RDF_FILE_FORMATS.keySet());
      }
      String format = request.queryParams("format");
      if (!RDF_FILE_FORMATS.keySet().contains(format)) {
        throw new Exception("Format unknown. Use any of: " + RDF_FILE_FORMATS.keySet());
      }
      RDFFormat applicationOntologyFormat = RDF_FILE_FORMATS.get(format);
      response.type(applicationOntologyFormat.getDefaultMIMEType());

      ModelBuilder builder = new ModelBuilder();
      WriterConfig config = new WriterConfig();
      config.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
      builder.setNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
      builder.setNamespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
      builder.setNamespace("owl", "http://www.w3.org/2002/07/owl#");
      builder.setNamespace("sio", "http://semanticscience.org/resource/");
      IRI schemaContext = iri(request.url());

      describeSchema(builder, schema, schemaContext);
      for (Table t : schema.getTablesSorted()) {
        describeTable(builder, t, schemaContext);
      }
      for (Table t : schema.getTablesSorted()) {
        describeColumns(builder, t, schemaContext);
      }

      Model model = builder.build();
      StringWriter stringWriter = new StringWriter();
      Rio.write(model, stringWriter, applicationOntologyFormat, config);
      printWriter.append(stringWriter.toString());

    } catch (Exception e) {
      throw new MolgenisException("RDF export failed", e);
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
}
