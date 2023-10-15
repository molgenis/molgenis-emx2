package org.molgenis.emx2.semantics;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.molgenis.emx2.semantics.ValueToRDF.columnTypeToXSD;
import static org.molgenis.emx2.semantics.ValueToRDF.describeValues;
import static org.molgenis.emx2.semantics.rdf.RootToRDF.describeRoot;

import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.molgenis.emx2.*;
import org.molgenis.emx2.semantics.rdf.IRIParsingEncoding;

// TODO check null value handling
// TODO check value types
// TODO make sure no classes are used as predicates and vice versa
// TODO: ontology tables need semantics to denote "what are these rows instances of?" (typeOf in FG)
// TODO: units for values?

/**
 * Nomenclature used from:
 *
 * <ul>
 *   <li>SIO http://semanticscience.org
 *   <li>RDF Data Cube https://www.w3.org/TR/vocab-data-cube
 *   <li>OWL, RDF, RDFS
 * </ul>
 */
public class RDFService {
  public static final Map<String, RDFFormat> RDF_FILE_FORMATS =
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
  private final RDFFormat rdfFormat;
  private final WriterConfig config;
  private final String host;

  public RDFService(String requestURL) {
    this(requestURL, null);
  }

  public RDFService(String requestURL, String format) {

    // reconstruct server:port URL to prevent problems with double encoding of schema/table names
    // etc
    URI requestURI = IRIParsingEncoding.getURI(requestURL);
    this.host = extractHost(requestURI);

    if (format == null) {
      this.rdfFormat = RDFFormat.TURTLE;
    } else {
      if (!RDF_FILE_FORMATS.containsKey(format)) {
        throw new MolgenisException("Format unknown. Use any of: " + RDF_FILE_FORMATS.keySet());
      }
      this.rdfFormat = RDF_FILE_FORMATS.get(format);
    }

    this.config = new WriterConfig();
    this.config.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
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
      Table table,
      String rowId,
      String columnName,
      Schema... schemas) {
    try {
      ModelBuilder builder = new ModelBuilder();
      builder.setNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
      builder.setNamespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
      builder.setNamespace("xsd", "http://www.w3.org/2001/XMLSchema#");
      builder.setNamespace("owl", "http://www.w3.org/2002/07/owl#");
      builder.setNamespace("sio", "http://semanticscience.org/resource/");
      builder.setNamespace("qb", "http://purl.org/linked-data/cube#");
      builder.setNamespace("dcterms", "http://purl.org/dc/terms/");
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

      Rio.write(builder.build(), outputStream, rdfFormat, config);

    } catch (Exception e) {
      throw new MolgenisException("RDF export failed due to an exception", e);
    }
  }

  /** Extract the host location from a request URI. */
  public static String extractHost(URI requestURI) {
    return requestURI.getScheme()
        + "://"
        + requestURI.getHost()
        + (requestURI.getPort() != -1 ? ":" + requestURI.getPort() : "");
  }

  public WriterConfig getConfig() {
    return config;
  }

  public String getHost() {
    return host;
  }

  public String getMimeType() {
    return rdfFormat.getDefaultMIMEType();
  }

  public RDFFormat getRdfFormat() {
    return rdfFormat;
  }

  public static void describeColumns(
      ModelBuilder builder, String columnName, Table table, String schemaContext) {
    String tableContext = schemaContext + "/" + table.getName();
    for (Column column : table.getMetadata().getColumns()) {
      if (columnName == null || column.getName().equals(columnName)) {
        describeColumn(builder, schemaContext, column, tableContext);
      }
    }
  }

  private static void describeColumn(
      ModelBuilder builder, String schemaContext, Column column, String tableContext) {
    String columnContext = tableContext + "/column/" + column.getName();
    // SIO:000757 = database column
    builder.add(columnContext, RDF.TYPE, iri("http://semanticscience.org/resource/SIO_000757"));
    builder.add(columnContext, RDF.TYPE, iri("http://purl.org/linked-data/cube#MeasureProperty"));
    if (column.isReference()) {
      builder.add(columnContext, RDF.TYPE, OWL.OBJECTPROPERTY);
      builder.add(
          columnContext,
          RDFS.RANGE,
          IRIParsingEncoding.encodedIRI(schemaContext + "/" + column.getRefTableName()));
    } else {
      builder.add(columnContext, RDF.TYPE, OWL.DATATYPEPROPERTY);
      builder.add(columnContext, RDFS.RANGE, columnTypeToXSD(column.getColumnType()));
    }
    builder.add(columnContext, RDFS.LABEL, column.getName());
    builder.add(columnContext, RDFS.DOMAIN, IRIParsingEncoding.encodedIRI(tableContext));
    if (column.getSemantics() != null) {
      for (String columnSemantics : column.getSemantics()) {
        if (columnSemantics.equals("id")) {
          // todo: need to figure out how to better handle 'id' tagging
          columnSemantics = "http://semanticscience.org/resource/SIO_000115";
        }
        builder.add(columnContext, RDFS.ISDEFINEDBY, iri(columnSemantics));
      }
    }
    if (column.getDescriptions() != null) {
      builder.add(columnContext, DC.DESCRIPTION, column.getDescriptions());
    }
  }

  public static void describeSchema(
      ModelBuilder builder, Schema schema, String schemaContext, String rootContext) {
    builder.add(schemaContext, RDFS.LABEL, schema.getName());
    builder.add(schemaContext, DCTERMS.IS_PART_OF, IRIParsingEncoding.encodedIRI(rootContext));
    if (schema.getMetadata().getDescription() != null) {
      builder.add(schemaContext, DCTERMS.DESCRIPTION, schema.getMetadata().getDescription());
    }
    builder.add(schemaContext, RDF.TYPE, RDFS.CONTAINER);
    for (String tableName : schema.getTableNames()) {
      IRI tableContext = IRIParsingEncoding.encodedIRI(schemaContext + "/" + tableName);
      builder.add(schemaContext, "http://www.w3.org/ns/ldp#contains", tableContext);
    }
  }

  public static void describeTable(ModelBuilder builder, Table table, String schemaContext) {
    IRI tableContext = IRIParsingEncoding.encodedIRI(schemaContext + "/" + table.getName());
    builder.add(tableContext, RDF.TYPE, OWL.CLASS);
    builder.add(tableContext, RDF.TYPE, iri("http://purl.org/linked-data/cube#DataSet"));
    // SIO:000754 = database table
    builder.add(tableContext, RDF.TYPE, iri("http://semanticscience.org/resource/SIO_000754"));
    if (table.getMetadata().getSemantics() != null) {
      for (String tableSemantics : table.getMetadata().getSemantics()) {
        builder.add(tableContext, RDFS.ISDEFINEDBY, iri(tableSemantics));
      }
    } else if (table.getMetadata().getTableType() == TableType.ONTOLOGIES) {
      builder.add(
          // NCIT:C48697 = Controlled Vocabulary
          tableContext, RDFS.ISDEFINEDBY, iri("http://purl.obolibrary.org/obo/NCIT_C48697"));
    } else {
      builder.add(
          // SIO:001055 = observing (definition: observing is a process of passive interaction in
          // which one entity makes note of attributes of one or more entities)
          tableContext, RDFS.ISDEFINEDBY, iri("http://semanticscience.org/resource/SIO_001055"));
    }
    builder.add(tableContext, RDFS.LABEL, table.getName());
    if (table.getMetadata().getDescriptions() != null
        && table.getMetadata().getDescriptions().get("en") != null) {
      builder.add(
          tableContext, DCTERMS.DESCRIPTION, table.getMetadata().getDescriptions().get("en"));
    }
    if (table.getMetadata().getTableType() == TableType.DATA) {
      // NCIT:C25474 = Data
      builder.add(tableContext, RDFS.RANGE, iri("http://purl.obolibrary.org/obo/NCIT_C25474"));
    } else if (table.getMetadata().getTableType() == TableType.ONTOLOGIES) {
      // NCIT:C21270 = Ontology
      builder.add(tableContext, RDFS.RANGE, iri("http://purl.obolibrary.org/obo/NCIT_C21270"));
    }
  }
}
