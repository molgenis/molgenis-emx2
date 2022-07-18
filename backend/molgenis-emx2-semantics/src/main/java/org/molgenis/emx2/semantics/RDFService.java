package org.molgenis.emx2.semantics;

import static org.eclipse.rdf4j.model.util.Values.iri;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.molgenis.emx2.*;
import org.molgenis.emx2.beaconv2.common.QueryHelper;
import spark.Request;
import spark.Response;

public class RDFService {
  private static ObjectMapper jsonMapper =
      new ObjectMapper()
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .setDateFormat(new StdDateFormat().withColonInTimeZone(true));

  public static Map<String, RDFFormat> FORMATS =
      new TreeMap<>(
          Map.of(
              "ttl",
              RDFFormat.TURTLE,
              "n3",
              RDFFormat.N3,
              "ntriples",
              RDFFormat.NTRIPLES,
              "nquads",
              RDFFormat.NQUADS,
              "xml",
              RDFFormat.RDFXML,
              "trig",
              RDFFormat.TRIG,
              "jsonld",
              RDFFormat.JSONLD));

  private RDFService() {
    // hidden
  }

  public static void getRdfForTable(
      Table table, PrintWriter writer, Request request, Response response) {

    try {
      String format = request.params("format");
      if (!FORMATS.keySet().contains(format)) {
        throw new Exception("Format unknown. Use any of: " + FORMATS.keySet());
      }
      RDFFormat applicationOntologyFormat = FORMATS.get(format);

      // TODO null checks?

      // Main model builder
      ModelBuilder builder = new ModelBuilder();
      ValueFactory vf = SimpleValueFactory.getInstance();
      WriterConfig config = new WriterConfig();
      config.set(BasicWriterSettings.INLINE_BLANK_NODES, true);

      String schemaContext = "http://localhost/" + table.getSchema().getName() + "/";
      String tableContext = schemaContext + table.getName();

      /*
      Schema-level metadata
       */
      builder.add(schemaContext, RDF.TYPE, RDFS.CONTAINER);
      builder.add(schemaContext, RDFS.RANGE, tableContext);
      builder.add(schemaContext, RDFS.LABEL, table.getSchema().getName());

      /*
      Table-level metadata
       */
      builder.add(tableContext, RDF.TYPE, OWL.CLASS);
      if (table.getMetadata().getSemantics() != null) {
        for (String tableSemantics : table.getMetadata().getSemantics()) {
          builder.add(tableContext, RDFS.ISDEFINEDBY, iri(tableSemantics));
        }
      } else {
        builder.add(tableContext, RDFS.ISDEFINEDBY, OWL.THING);
      }
      builder.add(tableContext, RDFS.LABEL, table.getName());
      builder.add(tableContext, RDFS.MEMBER, schemaContext);
      if (table.getMetadata().getTableType() == TableType.DATA) {
        builder.add(tableContext, RDFS.RANGE, iri("http://purl.obolibrary.org/obo/NCIT_C25474"));
      } else if (table.getMetadata().getTableType() == TableType.ONTOLOGIES) {
        builder.add(tableContext, RDFS.RANGE, iri("http://purl.obolibrary.org/obo/NCIT_C21270"));
      }

      /*
      Column-level metadata
       */
      for (Column c : table.getMetadata().getColumns()) {
        IRI columnContext = iri(tableContext + "/" + c.getName());

        // fixme: is isReference() same as isRef() ??
        if (c.isReference()) {
          builder.add(columnContext, RDF.TYPE, OWL.OBJECTPROPERTY);
        } else {
          builder.add(columnContext, RDF.TYPE, OWL.DATATYPEPROPERTY);
        }
        builder.add(columnContext, RDFS.LABEL, c.getName());
        builder.add(columnContext, RDFS.DOMAIN, iri(tableContext));
        if (c.getSemantics() != null) {
          for (String columnSemantics : c.getSemantics()) {
            builder.add(columnContext, RDFS.ISDEFINEDBY, iri(columnSemantics));
          }
        } else {
          builder.add(columnContext, RDFS.ISDEFINEDBY, OWL.THING);
        }
        builder.add(
            columnContext,
            DC.DESCRIPTION,
            c.getDescription() != null ? c.getDescription() : "None provided");
      }

      /*
      Value-level data
       */
      Query q = table.query();
      QueryHelper.selectColumns(table, q);
      String json = q.retrieveJSON();
      Map<String, List<Map<String, Object>>> jsonMap = jsonMapper.readValue(json, Map.class);
      List<Map<String, Object>> data = jsonMap.get(table.getName());
      for (Map<String, Object> map : data) {
        for (String key : map.keySet()) {
          if (map.get(key) != null) {
            IRI columnContext = iri(tableContext + "/" + key);
            builder.add(columnContext, OWL.HASVALUE, map.get(key));
          }
        }
      }

      Model model = builder.build();
      StringWriter stringWriter = new StringWriter();
      Rio.write(model, stringWriter, applicationOntologyFormat, config);
      writer.append(stringWriter.toString());

    } catch (Exception e) {
      throw new MolgenisException("RDF export failed", e);
    }
  }

  public static void getRdfForSchema(Schema schema, PrintWriter printWriter) {
    // todo
  }
}
