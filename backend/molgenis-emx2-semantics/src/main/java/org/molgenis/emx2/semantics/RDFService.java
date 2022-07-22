package org.molgenis.emx2.semantics;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.eclipse.rdf4j.model.vocabulary.XSD.*;
import static org.molgenis.emx2.semantics.ColumnTypeToXSDDataType.columnTypeToXSD;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.molgenis.emx2.*;
import org.molgenis.emx2.beaconv2.common.QueryHelper;
import spark.Request;
import spark.Response;

// TODO check null value handling
// TODO check value types
// TODO make sure no classes are used as predicates and vice versa
// TODO: ontology tables need semantics to denote "what are these rows instances of?" (typeOf in FG)

/**
 * Nomenclature used from:
 * <ul>
 *     <li>SIO (http://semanticscience.org)</li>
 *     <li>RDF Data Cube (https://www.w3.org/TR/vocab-data-cube)</li>
 *     <li>OWL, RDF, RDFS</li>
 * </ul>
 *
 *
 */
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
      if (request.queryParams("format") == null) {
        throw new Exception("Format not specified. Use any of: " + FORMATS.keySet());
      }
      String format = request.queryParams("format");
      if (!FORMATS.keySet().contains(format)) {
        throw new Exception("Format unknown. Use any of: " + FORMATS.keySet());
      }
      RDFFormat applicationOntologyFormat = FORMATS.get(format);
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
      describeValues(builder, table, schemaContext);

      Model model = builder.build();
      StringWriter stringWriter = new StringWriter();
      Rio.write(model, stringWriter, applicationOntologyFormat, config);
      writer.append(stringWriter.toString());

    } catch (Exception e) {
      throw new MolgenisException("RDF export failed", e);
    }
  }

  public static void getRdfForSchema(
      Schema schema, PrintWriter printWriter, Request request, Response response) {
    try {
      if (request.queryParams("format") == null) {
        throw new Exception(
            "Format not specified (using ?format=x). Use any of: " + FORMATS.keySet());
      }
      String format = request.queryParams("format");
      if (!FORMATS.keySet().contains(format)) {
        throw new Exception("Format unknown. Use any of: " + FORMATS.keySet());
      }
      RDFFormat applicationOntologyFormat = FORMATS.get(format);
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

  public static void describeSchema(ModelBuilder builder, Schema schema, IRI schemaContext) {
    builder.add(schemaContext, RDFS.LABEL, schema.getName());
    if (schema.getMetadata().getDescription() != null) {
      builder.add(schemaContext, DCTERMS.DESCRIPTION, schema.getMetadata().getDescription());
    }
    builder.add(schemaContext, RDF.TYPE, RDFS.CONTAINER);
    for (String tableName : schema.getTableNames()) {
      IRI tableContext = iri(schemaContext + "/" + tableName);
      builder.add(schemaContext, iri("http://www.w3.org/ns/ldp#contains"), tableContext);
    }
  }

  public static void describeTable(ModelBuilder builder, Table table, IRI schemaContext) {
    IRI tableContext = iri(schemaContext + "/" + table.getName());
    builder.add(tableContext, RDF.TYPE, OWL.CLASS);
    builder.add(tableContext, RDF.TYPE, iri("http://purl.org/linked-data/cube#DataSet"));
    builder.add(tableContext, RDF.TYPE, iri("http://semanticscience.org/resource/SIO_000754"));
    if (table.getMetadata().getSemantics() != null) {
      for (String tableSemantics : table.getMetadata().getSemantics()) {
        builder.add(tableContext, RDFS.ISDEFINEDBY, iri(tableSemantics));
      }
    } else if (table.getMetadata().getTableType() == TableType.ONTOLOGIES) {
      builder.add(tableContext, RDFS.ISDEFINEDBY, iri("http://purl.obolibrary.org/obo/NCIT_C48697"));
    }
    builder.add(tableContext, RDFS.LABEL, table.getName());
    if (table.getMetadata().getTableType() == TableType.DATA) {
      builder.add(tableContext, RDFS.RANGE, iri("http://purl.obolibrary.org/obo/NCIT_C25474"));
    } else if (table.getMetadata().getTableType() == TableType.ONTOLOGIES) {
      builder.add(tableContext, RDFS.RANGE, iri("http://purl.obolibrary.org/obo/NCIT_C21270"));
    }
  }

  // todo: unit is missing (which would also be a sdmx-attribute:unitMeasure, typed as an qb:AttributeProperty)
  public static void describeColumns(ModelBuilder builder, Table table, IRI schemaContext)
      throws Exception {
    IRI tableContext = iri(schemaContext + "/" + table.getName());
    for (Column c : table.getMetadata().getColumns()) {
      IRI columnContext = iri(tableContext + "/column/" + c.getName());
      builder.add(columnContext, RDF.TYPE, iri("http://semanticscience.org/resource/SIO_000757"));
      // fixme: is isReference() same as isRef() ??
      if (c.isReference()) {
        builder.add(columnContext, RDF.TYPE, OWL.OBJECTPROPERTY);
      } else {
        builder.add(columnContext, RDF.TYPE, OWL.DATATYPEPROPERTY);
      }
      builder.add(columnContext, RDF.TYPE, iri("http://purl.org/linked-data/cube#MeasureProperty"));
      builder.add(columnContext, RDFS.LABEL, c.getName());
      builder.add(columnContext, RDFS.DOMAIN, tableContext);
      if (c.getSemantics() != null) {
        for (String columnSemantics : c.getSemantics()) {
          builder.add(columnContext, RDFS.ISDEFINEDBY, iri(columnSemantics));
        }
      }
      if (c.getDescription() != null) {
        builder.add(columnContext, DC.DESCRIPTION, c.getDescription());
      }
      builder.add(columnContext, RDFS.RANGE, columnTypeToXSD(c.getColumnType()));
    }
  }

  public static void describeValues(
      ModelBuilder builder, Table table, IRI schemaContext) throws Exception {
    Map<String, Column> columnMap = new HashMap<>();
    for (Column c : table.getMetadata().getColumns()) {
      columnMap.put(c.getName(), c);
    }
    IRI tableContext = iri(schemaContext + "/" + table.getName());
    Query q = table.query();
    QueryHelper.selectColumns(table, q);
    String json = q.retrieveJSON();
    Map<String, List<Map<String, Object>>> jsonMap = jsonMapper.readValue(json, Map.class);
    List<Map<String, Object>> data = jsonMap.get(table.getName());

    for (Map<String, Object> row : data) {

      TableMetadata tableMetadata = table.getMetadata();
      String primaryKey = tableMetadata.getPrimaryKeys().get(0);
      String pkValue = (String) row.get(primaryKey);
      IRI rowContext = iri(schemaContext + "/" + table.getName() + "/row/" + pkValue);

      builder.add(rowContext, RDF.TYPE, tableContext);
      builder.add(rowContext, RDF.TYPE, iri("http://semanticscience.org/resource/SIO_001187"));
      builder.add(rowContext, RDF.TYPE, iri("http://purl.org/linked-data/cube#Observation"));
      builder.add(rowContext, iri("http://purl.org/linked-data/cube#dataSet"), tableContext);

      for (String column : row.keySet()) {
        if (row.get(column) != null) {
          IRI columnContext = iri(tableContext + "/column/" + column);
          for (Value value : formatValue(row.get(column), columnMap.get(column), schemaContext)) {
            builder.add(rowContext, columnContext, value);
          }
        }
      }
    }
  }

  public static Value[] formatValue(Object o, Column column, IRI schemaContext) throws Exception {
    Value[] valList;
    if (o instanceof List) {
      List<Object> objList = (List<Object>) o;
      valList = new Value[objList.size()];
      for (int i = 0; i < valList.length; i++) {
        valList[i] = applyFormatting(objList.get(i), column, schemaContext);
      }
    } else {
      valList = new Value[1];
      valList[0] = applyFormatting(o, column, schemaContext);
    }
    return valList;
  }

  public static Value applyFormatting(Object o, Column column, IRI schemaContext) throws Exception {
    ColumnType columnType = column.getColumnType();
    IRI XSDType = columnTypeToXSD(columnType);

    if (columnType.equals(ColumnType.ONTOLOGY) || columnType.equals(ColumnType.ONTOLOGY_ARRAY)) {
      return iri((String) ((Map) o).get("ontologyTermURI"));
    } else if (columnType.equals(ColumnType.REF)
        || columnType.equals(ColumnType.REF_ARRAY)
        || columnType.equals(ColumnType.REFBACK)) {
      // TODO should the target IRI be resolvable here?
      TableMetadata tableMetadata = column.getRefTable();
      String primaryKey = tableMetadata.getPrimaryKeys().get(0);
      String pkValue = (String) ((Map) o).get(primaryKey);
      return iri(schemaContext + "/" + tableMetadata.getTableName() + "/" + pkValue);

    } else {

      if (BOOLEAN.equals(XSDType)) {
        return literal((boolean) o);
      } else if (DATE.equals(XSDType)) {
        return literal(((String) o), XSDType);
      } else if (DATETIME.equals(XSDType)) {
        return literal(((String) o).substring(0, 19), XSDType);
      } else if (DECIMAL.equals(XSDType)) {
        return literal((double) o);
      } else if (STRING.equals(XSDType)) {
        return literal((String) o);
      } else if (ANYURI.equals(XSDType)) {
        return iri((String) o);
      } else if (INT.equals(XSDType)) {
        return literal((int) o);
      } else if (LONG.equals(XSDType)) {
        return literal((long) o);
      } else {
        throw new Exception("XSD type formatting not specified: " + XSDType);
      }
    }
  }
}
