package org.molgenis.emx2.rdf;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.molgenis.emx2.FilterBean.and;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;

import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.*;
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
  private static final Map<String, RDFFormat> RDF_FILE_FORMATS =
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
  private static final DateTimeFormatter dateTimeFormatter =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
  private final WriterConfig config;
  private final RDFFormat rdfFormat;
  private final String host;

  public RDFService(String requestURL) {
    this(requestURL, null);
  }

  public RDFService(String requestURL, String format) {

    // reconstruct server:port URL to prevent problems with double encoding of schema/table names
    // etc
    URI requestURI = getURI(requestURL);
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
            rowsToRdf(builder, tableToDescribe, rowId, schemaRdfApiContext);
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

  // todo: make non static
  public static void describeRoot(ModelBuilder builder, String rootContext) {
    // SIO:000750 = database
    builder.add(rootContext, RDF.TYPE, iri("http://semanticscience.org/resource/SIO_000750"));
    builder.add(rootContext, RDFS.LABEL, "EMX2");
    builder.add(rootContext, DCTERMS.DESCRIPTION, "MOLGENIS EMX2 database at " + rootContext);
    builder.add(rootContext, DCTERMS.CREATOR, iri("https://molgenis.org"));
  }

  // todo: make non-static
  public static void describeSchema(
      ModelBuilder builder, Schema schema, String schemaContext, String rootContext) {
    builder.add(schemaContext, RDFS.LABEL, schema.getName());
    builder.add(schemaContext, DCTERMS.IS_PART_OF, encodedIRI(rootContext));
    if (schema.getMetadata().getDescription() != null) {
      builder.add(schemaContext, DCTERMS.DESCRIPTION, schema.getMetadata().getDescription());
    }
    builder.add(schemaContext, RDF.TYPE, RDFS.CONTAINER);
    for (String tableName : schema.getTableNames()) {
      IRI tableContext = encodedIRI(schemaContext + "/" + tableName);
      builder.add(schemaContext, "http://www.w3.org/ns/ldp#contains", tableContext);
    }
  }

  public static void describeTable(ModelBuilder builder, Table table, String schemaContext) {
    IRI tableContext = encodedIRI(schemaContext + "/" + table.getName());
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
          columnContext, RDFS.RANGE, encodedIRI(schemaContext + "/" + column.getRefTableName()));
    } else {
      builder.add(columnContext, RDF.TYPE, OWL.DATATYPEPROPERTY);
      builder.add(columnContext, RDFS.RANGE, columnTypeToXSD(column.getColumnType()));
    }
    builder.add(columnContext, RDFS.LABEL, column.getName());
    builder.add(columnContext, RDFS.DOMAIN, encodedIRI(tableContext));
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

  public static CoreDatatype.XSD columnTypeToXSD(ColumnType columnType) {
    switch (columnType) {
      case BOOL, BOOL_ARRAY:
        return CoreDatatype.XSD.BOOLEAN;
      case DATE, DATE_ARRAY:
        return CoreDatatype.XSD.DATE;
      case DATETIME, DATETIME_ARRAY:
        return CoreDatatype.XSD.DATETIME;

      case DECIMAL, DECIMAL_ARRAY:
        return CoreDatatype.XSD.DECIMAL;

      case EMAIL,
          EMAIL_ARRAY,
          HEADING,
          JSONB,
          JSONB_ARRAY,
          STRING,
          STRING_ARRAY,
          TEXT,
          TEXT_ARRAY,
          UUID,
          UUID_ARRAY,
          AUTO_ID:
        return CoreDatatype.XSD.STRING;

      case FILE, HYPERLINK, HYPERLINK_ARRAY, ONTOLOGY, ONTOLOGY_ARRAY, REF, REF_ARRAY, REFBACK:
        return CoreDatatype.XSD.ANYURI;

      case INT, INT_ARRAY:
        return CoreDatatype.XSD.INT;

      case LONG, LONG_ARRAY:
        return CoreDatatype.XSD.LONG;

      default:
        throw new MolgenisException("ColumnType not mapped: " + columnType);
    }
  }

  public static URI getURI(String uriString) {
    try {
      ParsedIRI parsedIRI = ParsedIRI.create(uriString);
      return new URI(
          parsedIRI.getScheme(),
          parsedIRI.getUserInfo(),
          parsedIRI.getHost(),
          parsedIRI.getPort(),
          parsedIRI.getPath(),
          parsedIRI.getQuery(),
          parsedIRI.getFragment());
    } catch (Exception e) {
      throw new MolgenisException("getURI failed", e);
    }
  }

  /**
   * @param uriString
   * @return
   */
  public static IRI encodedIRI(String uriString) {
    return org.eclipse.rdf4j.model.util.Values.iri(ParsedIRI.create(uriString).toString());
  }

  public static void rowsToRdf(
      ModelBuilder builder, Table table, String rowId, String schemaContext) {
    Map<String, Column> columnMap = new HashMap<>();
    for (Column c : table.getMetadata().getColumns()) {
      columnMap.put(c.getName(), c);
    }
    String tableContext = schemaContext + "/" + table.getName();
    for (Row row : getRows(table, rowId)) {
      IRI rowContext =
          getIriValuesBasedOnPkey(schemaContext, table.getMetadata(), row, "")
              .get(0); // note the prefix
      builder.add(rowContext, RDF.TYPE, encodedIRI(tableContext));
      // SIO:001187 = database row
      builder.add(rowContext, RDF.TYPE, iri("http://semanticscience.org/resource/SIO_001187"));
      if (table.getMetadata().getTableType() == TableType.ONTOLOGIES) {
        // NCIT:C95637 = Coded Value Data Type
        builder.add(rowContext, RDF.TYPE, iri("http://purl.obolibrary.org/obo/NCIT_C95637"));
        if (row.getString("ontologyTermURI") != null) {
          builder.add(rowContext, RDFS.ISDEFINEDBY, iri(row.getString("ontologyTermURI")));
        }
      } else {
        builder.add(rowContext, RDF.TYPE, iri("http://purl.org/linked-data/cube#Observation"));
      }
      builder.add(
          rowContext, iri("http://purl.org/linked-data/cube#dataSet"), encodedIRI(tableContext));

      for (Column column : table.getMetadata().getColumns()) {
        IRI columnContext = encodedIRI(tableContext + "/column/" + column.getName());
        for (Value value : formatValue(row, column, schemaContext)) {
          builder.add(rowContext, columnContext, value);
        }
      }
    }
  }

  private static List<Row> getRows(Table table, String rowId) {
    Query query = table.query();
    if (rowId != null) {
      if (table.getMetadata().getPrimaryKeyFields().size() > 1) {
        query.where(decodeRowIdToFilter(rowId));
      } else {
        query.where(f(table.getMetadata().getPrimaryKeyColumns().get(0).getName(), EQUALS, rowId));
      }
    }
    return query.retrieveRows(); // we use the default select
  }

  private static Filter decodeRowIdToFilter(String rowId) {
    try {
      List<NameValuePair> params =
          URLEncodedUtils.parse(new URI("?" + rowId), StandardCharsets.UTF_8);
      List<Filter> filters = new ArrayList<>();
      params.forEach(param -> filters.add(f(param.getName(), EQUALS, param.getValue())));
      return and(filters);
    } catch (Exception e) {
      throw new MolgenisException("Decode row to filter failed for id " + rowId);
    }
  }

  private static List<IRI> getIriValuesBasedOnPkey(
      String schemaContext, TableMetadata tableMetadata, Row row, String prefix) {
    String[] keys =
        row.getStringArray(prefix + tableMetadata.getPrimaryKeyFields().get(0).getName());
    // check null
    if (keys == null) {
      return List.of();
    }
    // simple keys get the key value
    if (tableMetadata.getPrimaryKeyFields().size() == 1) {
      return Arrays.stream(keys)
          .map(
              value -> encodedIRI(schemaContext + "/" + tableMetadata.getTableName() + "/" + value))
          .toList();
    }
    // composite keys get pattern of part1=a&part2=b
    else {
      List<List<NameValuePair>> keyValuePairList = new ArrayList<>();
      tableMetadata.getPrimaryKeyFields().stream()
          .forEach(
              field -> {
                String fieldName = prefix + field.getName();
                String[] keyParts = row.getStringArray(fieldName);
                for (int i = 0; i < keyParts.length; i++) {
                  if (keyValuePairList.size() <= i) keyValuePairList.add(new ArrayList<>());
                  keyValuePairList.get(i).add(new BasicNameValuePair(fieldName, keyParts[i]));
                }
              });
      return keyValuePairList.stream()
          .map(
              valuePairList ->
                  encodedIRI(
                      schemaContext
                          + "/"
                          + tableMetadata.getTableName()
                          + "/"
                          + URLEncodedUtils.format(valuePairList, StandardCharsets.UTF_8)))
          .toList();
    }
  }

  public static List<Value> formatValue(Row row, Column column, String schemaContext) {
    List<Value> values = new ArrayList<>();
    ColumnType columnType = column.getColumnType();
    if (columnType.isReference()) {
      values.addAll(
          getIriValuesBasedOnPkey(
              schemaContext, column.getRefTable(), row, column.getName() + "."));
    } else if (columnType.equals(ColumnType.FILE)) {
      if (row.getString(column.getName() + "_id") != null) {
        values.add(
            encodedIRI(
                schemaContext
                    + "/api/file/"
                    + column.getTableName()
                    + "/"
                    + column.getName()
                    + "/"));
      }
    } else {
      values.addAll(getLiteralValues(row, column));
    }
    return values;
  }

  public static List<Value> getLiteralValues(Row row, Column column) {
    CoreDatatype.XSD xsdType = columnTypeToXSD(column.getColumnType());
    if (row.getString(column.getName()) == null) {
      return List.of();
    }
    switch (xsdType) {
      case BOOLEAN:
        return Arrays.stream(row.getBooleanArray(column.getName()))
            .map(value -> (Value) literal(value))
            .toList();
      case DATE:
        return Arrays.stream(row.getDateArray(column.getName()))
            .map(value -> (Value) literal(value.toString(), xsdType))
            .toList();
      case DATETIME:
        return Arrays.stream(row.getDateTimeArray(column.getName()))
            .map(value -> (Value) literal(dateTimeFormatter.format(value), xsdType))
            .toList();
      case DECIMAL:
        return Arrays.stream(row.getDecimalArray(column.getName()))
            .map(value -> (Value) literal(value))
            .toList();
      case STRING:
        return Arrays.stream(row.getStringArray(column.getName()))
            .map(value -> (Value) literal(value))
            .toList();
      case ANYURI:
        return Arrays.stream(row.getStringArray(column.getName()))
            .map(value -> (Value) encodedIRI(value))
            .toList();
      case INT:
        return Arrays.stream(row.getIntegerArray(column.getName()))
            .map(value -> (Value) literal(value))
            .toList();
      case LONG:
        return Arrays.stream(row.getLongArray(column.getName()))
            .map(value -> (Value) literal(value))
            .toList();
      default:
        throw new MolgenisException("XSD type formatting not supported for: " + xsdType);
    }
  }
}
